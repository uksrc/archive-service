package org.uksrc.archive.utils;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.javastro.ivoa.entities.uws.ExecutionPhase;
import org.javastro.ivoacore.tap.TAPJob;
import org.javastro.ivoacore.tap.TAPJobSpecification;
import org.javastro.ivoacore.tap.TAPWriter;
import org.javastro.ivoacore.uws.UWSException;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.RestQuery;
import org.jboss.resteasy.reactive.server.multipart.MultipartFormDataInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.RowListStarTable;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.time.Duration;
import java.util.Map;

@Tag(name = "TAP Query", description = "the TAP query endpoints")
@ApplicationScoped
@Path("sync")
public class TapSyncResource {
        @ConfigProperty(name="ivoa.tap.sync-timeout-seconds", defaultValue = "5")
        int syncTimeoutSeconds;

        @Inject
        TAPHelper  tapHelper;

        private static final Logger log = LoggerFactory.getLogger(TapSyncResource.class);

        @GET
        @Produces("application/x-votable+xml")
        public Uni<java.nio.file.Path> syncGet(@RestQuery String query, @RestQuery String lang, @RestQuery String responseformat, @RestQuery Long maxrec, @RestQuery String runid,
                                               @RestQuery String upload,
                                               @Context UriInfo uriInfo) {
            Map<String, URI> uploadMap = Utilities.parseUploadParams(upload, null);
            return runAndCleanupJob(query, lang, responseformat, maxrec, runid, uriInfo, uploadMap);
        }

        //UPLOAD param details - https://www.ivoa.net/documents/DALI/20170517/REC-DALI-1.1.html#tth_sEc3.4.5
        //UPLOAD=table1,http://example.com/t1.xml
        //UPLOAD=image1,vos://example.authority!tempSpace/foo.fits
        //UPLOAD=table3,param:t3
        @POST
        @Consumes({MediaType.APPLICATION_FORM_URLENCODED, MediaType.MULTIPART_FORM_DATA})
        @Produces("application/x-votable+xml")
        public Uni<java.nio.file.Path> syncPost(@RestForm("QUERY") String query, @RestForm("LANG") String lang, @RestForm("RESPONSEFORMAT") String responseformat, @RestForm("MAXREC") Long maxrec, @RestForm("RUNID") String runid,
                                                @RestForm("UPLOAD") String upload,
                                                MultipartFormDataInput input,
                                                @Context UriInfo uriInfo) {

            Map<String, URI> uploadMap = Utilities.parseUploadParams(upload, input);

            return runAndCleanupJob(query, lang, responseformat, maxrec, runid, uriInfo, uploadMap);
        }

        /**
         * Executes a job based on the provided parameters and ensures that temporary upload files are cleaned up after completion.
         * The method invokes a TAP job, listens for its termination, and performs cleanup of uploaded files stored locally.
         *
         * @param query The query string detailing the request to be processed.
         * @param lang The language in which the query is written.
         * @param responseformat Specifies the format in which the response should be returned.
         * @param maxrec The maximum number of records to be returned by the query.
         * @param runid A unique identifier for the job run.
         * @param uriInfo The URI context of the request.
         * @param uploadMap A map of upload identifiers to their respective URIs for uploaded resources.
         * @return A Uni containing the path to the resulting file of the job execution.
         */
        private Uni<java.nio.file.Path> runAndCleanupJob(@RestQuery String query, @RestQuery String lang, @RestQuery String responseformat, @RestQuery Long maxrec, @RestQuery String runid, @Context UriInfo uriInfo, Map<String, URI> uploadMap) {
            return handleJob(query, lang, responseformat, maxrec, runid, uploadMap, uriInfo)
                    .onTermination()
                    .invoke(() -> {
                        for (URI uploadedFile : uploadMap.values()) {
                            if ("file".equalsIgnoreCase(uploadedFile.getScheme())) {
                                try {
                                    Files.deleteIfExists(java.nio.file.Path.of(uploadedFile));
                                } catch (IOException e) {
                                    log.warn("Failed to delete upload file {}", uploadedFile, e);
                                }
                            }
                        }
                    });
        }

        private Uni<java.nio.file.Path> handleJob(String query, String lang, String responseformat, Long maxrec, String runid, Map<String, URI> uploads, UriInfo uriInfo) {
            final Duration SYNC_WAIT = Duration.ofSeconds(syncTimeoutSeconds);
            return Uni.createFrom().deferred(() -> {
                final TAPJob job;
                try {
                    job = (TAPJob) tapHelper.jobmanager.createJob(
                            new TAPJobSpecification(query, lang, responseformat, maxrec, runid, uploads)
                    );

                    tapHelper.jobmanager.runJob(job.getID()); // automatically run the job
                } catch (UWSException e) {
                    return Uni.createFrom().failure(e);
                }

                return Uni.createFrom().completionStage(job.getJobFuture())
                        .onItem().transformToUni(phase -> {
                                    if (phase == ExecutionPhase.COMPLETED) {
                                        return successResponse(job);
                                    }
                                    else if (phase == ExecutionPhase.ERROR)
                                    {
                                        return Uni.createFrom().item( buildErrorVOTable(job,null, false));
                                    }
                                    else {
                                        return Uni.createFrom().failure(new UWSException("Underlying TAP job completed with unexpected phase " + phase));//TODO could do more sophisticated error handling here based on the phase
                                    }
                                }
                        )
                        .ifNoItem().after(SYNC_WAIT)
                        .recoverWithItem(
                                buildErrorVOTable(job, new UWSException("query did not complete within sync time limit of " + SYNC_WAIT.toSeconds() + " seconds - continuing as UWS job"), true)//FIXME should this return http error code - if so which code?
                        );

            }).runSubscriptionOn(Infrastructure.getDefaultExecutor()); //TODO review whether this is the right way to do this - We might want to use a dedicated thread pool for this or some other strategy for managing the threads.
        }

        private Uni<java.nio.file.Path> successResponse(TAPJob job) {
            return Uni.createFrom().item(() -> {
                try {
                    return tapHelper.getResultPath(job.getID());
                } catch (UWSException e) {
                    throw new RuntimeException("Failed to get result path for job " + job.getID(), e);
                }
            });
        }

        //TODO do we always want to return a VOTable even for errors? Or should we allow some other error response?
        //TODO perhaps some of this can be moved to the TAPJob itself (for dealing with other types of errors - e.g. failure to parse original query)
        protected java.nio.file.Path buildErrorVOTable(TAPJob job, UWSException exception, boolean timeout) {
            // create a VOTable with STIL that has the error message and return the path to it. We could also include some info from the job if we have it.

            TAPJobSpecification tapJobSpec = (TAPJobSpecification) job.getJobSpecification();
            try {

                final TAPWriter tableWriter = new TAPWriter(job);
                ColumnInfo[] columns = new ColumnInfo[]{
                        new ColumnInfo("ERROR", String.class, "TAP error message")
                };
                RowListStarTable table = new RowListStarTable(columns);
                table.setName("error");
                if(exception != null) {
                    table.addRow(new Object[]{exception.getMessage()});
                }
                if(timeout) {
                    tableWriter.setTimeoutInfo(tapHelper.asyncJobUri(job.getID()));
                }

                java.nio.file.Path tempFile = java.nio.file.Files.createTempFile("error", ".vot");
                try (java.io.OutputStream out = java.nio.file.Files.newOutputStream(tempFile)) {

                    tableWriter.writeStarTable(table, out);
                }
                return tempFile;
            } catch (java.io.IOException e) {
                throw new RuntimeException("Failed to create error VOTable: " + e.getMessage(), e);
            }
        }
    }



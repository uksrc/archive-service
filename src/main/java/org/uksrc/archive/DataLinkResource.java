package org.uksrc.archive;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import org.apache.tika.mime.MimeTypeException;
import org.apache.tika.mime.MimeTypes;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.ParameterIn;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameters;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.ivoa.dm.caom2.Artifact;
import org.jboss.logging.Logger;
import org.uksrc.archive.auth.ConditionalRolesAllowed;
import org.uksrc.archive.datalink.VOTableGenerator;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

@Path("/datalink")
public class DataLinkResource {

    @Inject
    VOTableGenerator voTableGenerator;

    @PersistenceContext
    protected EntityManager em;

    Logger logger = Logger.getLogger(DataLinkResource.class);

    @GET
    @Path("/links")
    @Operation(summary = "Gets a DataLink object.", description = "Returns the DataLink object for the supplied observation ID.")
    @Parameters({
            @Parameter(
                    name = "ID",
                    description = "The observation to query.",
                    in = ParameterIn.QUERY,
                    required = true,
                    schema = @Schema(type = SchemaType.STRING)
            )
    })
    @APIResponse(
            responseCode = "200",
            description = "DataLink object (VOTable) for the supplied Observation.Id",
            content = {
                    @Content(
                            mediaType = MediaType.APPLICATION_XML, schema = @Schema(type = SchemaType.STRING, format = "xml")
                    )
            }
    )
    @APIResponse(
            responseCode = "500",
            description = "Internal error whilst retrieving Observation (or parameter error (if supplied))."
    )
    @Produces(MediaType.APPLICATION_XML)
    public Response getDataLinkObject(@QueryParam("ID") String id) {
        StreamingOutput out = voTableGenerator.createDocument(id);
        if (out != null) {
            return Response.ok(out).build();
        }
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Error, could not construct DataLink VOTable")
                .type(MediaType.TEXT_PLAIN)
                .build();
    }

    @GET
    @Path("/resource/{artifactId}")
    @Operation(summary = "Returns a resource.", description = "Returns the resource specified by the access_url of a DataLink VOTable.")
    @Parameters({
            @Parameter(
                    name = "ID",
                    description = "The ID defined in the access_url of a DataLink VOTable.",
                    in = ParameterIn.PATH,
                    required = true,
                    schema = @Schema(type = SchemaType.STRING)
            )
    })
    @APIResponse(
            responseCode = "200",
            description = "Content stream of the actual resource.",
            content = {
                    @Content(
                            mediaType = MediaType.APPLICATION_OCTET_STREAM, schema = @Schema(type = SchemaType.STRING, format = "binary")
                    )
            }
    )
    @APIResponse(
            responseCode = "404",
            description = "If the supplied resource ID cannot be found."
    )
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @ConditionalRolesAllowed("resource.roles.view")
    public Response getResource(@PathParam("artifactId") String id) {
        //Expects the Artifact.Id as the path parameter but actually returns the resource defined via the Artifact.uri
        Artifact art = findArtifact(id);
        if (art == null) {
            return notFound("Artifact " + id + " not found");
        }

        String resourceUri = art.getUri();
        if (!resourceExists(resourceUri)) {
            return notFound("Associated resource not found for " + id);
        }

        StreamingOutput stream = createStream(resourceUri);
        String filename = createFileName(id, art.getContentType());

        return Response.ok(stream, art.getContentType())
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .build();
    }

    /**
     * Search for an artifact with a given ID
     * @param id The ID of the artifact to find (Entity.id)
     * @return The artifact or null if not found.
     */
    private Artifact findArtifact(String id) {
        TypedQuery<Artifact> existsQuery = em.createQuery(
                "SELECT a FROM Artifact a WHERE a.id = :id", Artifact.class
        );

        try {
            existsQuery.setParameter("id", id);
            return existsQuery.getSingleResult();
        } catch (NoResultException e){
            return null;
        }
    }

    /**
     * Creates an output stream for the supplied URI
     * @param uri The location of the resource to return.
     * @return StreamingOutput of the actual resource.
     */
    private StreamingOutput createStream(String uri) {
        return output -> {
            try (InputStream is = new URL(uri).openStream();
                 OutputStream os = output) {
                is.transferTo(os);
            } catch (IOException e) {
                throw new WebApplicationException("Failed to stream file: " + uri,
                        Response.Status.INTERNAL_SERVER_ERROR);
            }
        };
    }

    /**
     * Creates a filename for output based on the artifact ID and the mimeType
     * @param resourceId The Artifact ID
     * @param contentType The mimeType of the resource
     * @return A String in the format {resourceId}.{mimeType extension} - OR {resourceId} if no extension can be resolved.
     */
    private String createFileName(String resourceId, String contentType) {
        String filename = resourceId;
        try {
            String ext = MimeTypes.getDefaultMimeTypes()
                    .forName(contentType)
                    .getExtension();
            filename = filename + ext;
        } catch (MimeTypeException e) {
            logger.error("DataLink:Failed to determine mimetype", e);
        }
        return filename;
    }

    /**
     * Tests whether a resource at the specified URI actually exists.
     * @param uri The uri of the resource.
     * @return true if found
     */
    private boolean resourceExists(String uri) {
        URLConnection conn = null;
        try {
            URL url = new URL(uri);
            conn = url.openConnection();
            conn.connect();
        } catch (IOException e) {
            logger.error("DataLink: unable to resolve Artifact URI " + uri, e);
            return false;
        }
        return true;
    }

    /**
     * Generic "not found" message handler
     * @param message The message to return to the caller.
     * @return Response containing a NOT_FOUND message.
     */
    private Response notFound(String message) {
        return Response.status(Response.Status.NOT_FOUND)
                .type(MediaType.TEXT_PLAIN)
                .entity(message)
                .build();
    }
}

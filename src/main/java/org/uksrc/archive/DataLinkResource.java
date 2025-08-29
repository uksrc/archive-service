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
import org.ivoa.dm.caom2.Artifact;
import org.uksrc.archive.datalink.VOTableGenerator;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

@Path("/datalink")
public class DataLinkResource {

    @Inject
    VOTableGenerator voTableGenerator;

    @PersistenceContext
    protected EntityManager em;

    @GET
    @Path("/links")
    @Produces(MediaType.APPLICATION_XML)
    public Response getDataLinkedObject(@QueryParam("ID") String id){
        //TODO match ID with database object

        //"2cf99e88-90e1-4fe8-a502-e5cafdc6ffa1"
        StreamingOutput out = voTableGenerator.createDocument(id);
        return Response.ok(out).build();
    }

    @GET
    @Path("/files/{artifactId}")
    @Produces(MediaType.APPLICATION_XML)
    public Response getResource(@PathParam("artifactId") String id){
        Artifact art = findArtifact(id);
        if(art != null){
            //NOTE: Uri() assumed to be a fixed/resolvable fileUrl (or https) until defined differently
            String fileUri = art.getUri();

            //Stream file
            StreamingOutput stream = output -> {
                try (InputStream is = new URL(fileUri).openStream();
                     OutputStream os = output) {
                    is.transferTo(os);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to stream file", e);
                }
            };

            return Response.ok(stream)
                    .header("Content-Disposition", "attachment; filename=\"image.png\"")
                    .build();
        } else {
            return Response.status(Response.Status.NOT_FOUND)
                    .type(MediaType.TEXT_PLAIN)
                    .entity("Resource not found")
                    .build();
        }
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
}

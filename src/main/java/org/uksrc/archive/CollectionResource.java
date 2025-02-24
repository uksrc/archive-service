package org.uksrc.archive;

import jakarta.annotation.security.RolesAllowed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.uksrc.archive.utils.responses.Responses;
import org.uksrc.archive.utils.tools.Tools;

import java.util.List;

@SuppressWarnings("unused")
@Path("/collections")
@RolesAllowed("default-role-archive-service")
public class CollectionResource {

    @PersistenceContext
    protected EntityManager em;

    @GET
    @Path("/")
    @Operation(summary = "Retrieve all collection IDs", description = "Returns a list of unique collectionIds as a TSV (Tab Separated List).")
    @APIResponse(
            responseCode = "200",
            description = "CollectionIds retrieved successfully",
            content = @Content(
                    mediaType = MediaType.TEXT_PLAIN, schema = @Schema(implementation = String.class)
            )
    )
    @APIResponse(
            responseCode = "400",
            description = "Internal error whilst retrieving collectionIds."
    )
    @Produces(MediaType.TEXT_PLAIN)
    public Response getCollections(){
        try {
            TypedQuery<String> query = em.createQuery("SELECT DISTINCT o.collection FROM Observation o", String.class);
            List<String> uniqueCollections = query.getResultList();

            return Response.ok()
                    .type(MediaType.TEXT_PLAIN)
                    .entity(Tools.convertListToTsv(uniqueCollections))
                    .build();
        } catch (Exception e) {
            return Responses.errorResponse(e);
        }
    }
}

package org.uksrc.archive;
/*
 * Created on 21/08/2024 by Paul Harrison (paul.harrison@manchester.ac.uk).
 */

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.ivoa.dm.caom2.caom2.DerivedObservation;
import org.ivoa.dm.caom2.caom2.Observation;
import org.ivoa.dm.caom2.caom2.SimpleObservation;

import java.util.*;


@Path("/observations")
public class ObservationResource {

    @PersistenceContext
    protected EntityManager em;  // exists for the application lifetime no need to close

    @PUT
    @Path("/add")
    @Operation(summary = "Create a new Observation", description = "Creates a new observation in the database, note the supplied ID needs to be unique.")
    @RequestBody(
            description = "XML representation of the Observation",
            required = true,
            content = @Content(
                    mediaType = MediaType.APPLICATION_XML,
                    schema = @Schema(implementation = SimpleObservation.class)
            )
    )
    @APIResponse(
            responseCode = "201",
            description = "Observation created successfully",
            content = @Content(schema = @Schema(implementation = SimpleObservation.class))
    )
    @APIResponse(
            responseCode = "400",
            description = "Invalid input"
    )
    @Consumes(MediaType.APPLICATION_XML)
    @Produces(MediaType.APPLICATION_XML)
    @Transactional
    public Response addObservation(SimpleObservation observation) {
        try {
            em.persist(observation);
            em.flush();                 //Forced transaction to catch duplicate keys errors more gracefully.
        } catch (Exception e) {
            return errorResponse(e);
        }
        //return observation;
        return Response.status(Response.Status.CREATED)
                .entity(observation)
                .build();
    }

    @PUT
    @Path("/derived/add")
    @Operation(summary = "Create a new Derived Observation", description = "Create a DERIVED observation in the database, note ID must be unique across all observations.")
    @RequestBody(
            description = "XML representation of the Derived Observation",
            required = true,
            content = @Content(
                    mediaType = MediaType.APPLICATION_XML,
                    schema = @Schema(implementation = DerivedObservation.class)
            )
    )
    @APIResponse(
            responseCode = "201",
            description = "Observation created successfully",
            content = @Content(schema = @Schema(implementation = DerivedObservation.class))
    )
    @APIResponse(
            responseCode = "400",
            description = "Invalid input"
    )
    @Consumes(MediaType.APPLICATION_XML)
    @Produces(MediaType.APPLICATION_XML)
    @Transactional
    public Response addObservation(DerivedObservation observation) {
        try {
            em.persist(observation);
            em.flush();
        } catch (Exception e) {
            return errorResponse(e);
        }
        return Response.status(Response.Status.CREATED)
                .entity(observation)
                .build();
    }

    @POST
    @Path("/update/{observationId}")
    @Operation(summary = "Update an existing Observation", description = "Updates an existing observation with the supplied ID")
    @Parameter(
            name = "observationId",
            description = "ID of the Observation to be updated",
            required = true,
            example = "123"
    )
    @RequestBody(
            description = "XML representation of the Derived Observation",
            required = true,
            content = @Content(
                    mediaType = MediaType.APPLICATION_XML,
                    schema = @Schema(implementation = Observation.class)
            )
    )
    @APIResponse(
            responseCode = "200",
            description = "Observation updated successfully",
            content = @Content(schema = @Schema(implementation = Observation.class))
    )
    @APIResponse(
            responseCode = "404",
            description = "An Observation with the supplied ID has not been found."
    )
    @APIResponse(
            responseCode = "400",
            description = "Invalid input"
    )
    @Consumes(MediaType.APPLICATION_XML)
    @Produces(MediaType.APPLICATION_XML)
    @Transactional
    public Response updateObservation(@PathParam("observationId") String id, SimpleObservation observation) {
        try {
            //Only update IF found
            Observation existing = em.find(Observation.class, id);
            if (existing != null && observation != null) {
                observation.setId(id);
                em.merge(observation);
                return Response.ok(observation).build();
            }
        } catch (Exception e) {
            return errorResponse(e);
        }

        return Response.status(Response.Status.NOT_FOUND)
                .entity("Observation not found")
                .build();
    }

    @GET
    @Path("/")
    @Operation(summary = "Retrieve all observations")
    @Produces(MediaType.APPLICATION_XML)
    public ObservationListWrapper getAllObservations() {
        TypedQuery<Observation> query = em.createQuery("SELECT o FROM Observation o", Observation.class);
      //  query.setMaxResults(10); //TODO pagination
        List<Observation> observations = query.getResultList();
        return new ObservationListWrapper(observations);
    }

    @GET
    @Path("/{collection}")
    @Operation(summary = "Retrieve observations from a collection")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Observation> getObservations(@PathParam("collection") String collection) {
        TypedQuery<Observation> query = em.createQuery("SELECT o FROM Observation o WHERE o.collection = :collection", Observation.class);
        query.setParameter("collection", collection);
        return query.getResultList();
    }

    @DELETE
    @Path("/delete/{observationId}")
    @Operation(summary = "Delete an existing observation")
    @Transactional
    public Response deleteObservation(@PathParam("observationId") String id) {
        try {
            Observation observation = em.find(Observation.class, id);
            if (observation != null) {
                em.remove(observation);
                return Response.status(Response.Status.NO_CONTENT).build();
            }
        } catch (Exception e) {
            return errorResponse(e);
        }

        return Response.status(Response.Status.NOT_FOUND)
                .entity("Observation with ID " + id + " not found")
                .build();
    }

    private Response errorResponse (Exception e){
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(e.getMessage())
                .build();
    }
}

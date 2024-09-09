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
    @Operation(summary = "Create a new Observation")
    @Consumes(MediaType.APPLICATION_XML)
    @Transactional
    public Observation addObservation(SimpleObservation observation) {
        em.persist(observation);
        return observation;
    }

    @PUT
    @Path("/derived/add")
    @Operation(summary = "Create a new Derived Observation")
    @Consumes(MediaType.APPLICATION_XML)
    @Transactional
    public Observation addObservation(DerivedObservation observation) {
        em.persist(observation);
        return observation;
    }

    @POST
    @Path("/update")
    @Operation(summary = "Update an existing Observation, based on id")
    @Consumes(MediaType.APPLICATION_XML)
    @Transactional
    public Response updateObservation(SimpleObservation observation) {
        //Only update IF found
        Observation existing = em.find(Observation.class, observation.getId());
        if (existing != null) {
            em.merge(observation);
            return Response.ok(observation).build();
        }
        return Response.status(Response.Status.NOT_FOUND)
                .entity("Observation not found")
                .build();
    }

    @GET
    @Path("/")
    @Operation(summary = "Retrieve all observations")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Observation> getAllObservations() {
        TypedQuery<Observation> query = em.createQuery("SELECT o FROM Observation o", Observation.class);
      //  query.setMaxResults(10); //TODO pagination
        return query.getResultList();
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
        Observation observation = em.find(Observation.class, id);
        if (observation != null) {
            em.remove(observation);
            return Response.status(Response.Status.NO_CONTENT).build();
        }
        return Response.status(Response.Status.NOT_FOUND)
                .entity("Observation with ID " + id + " not found")
                .build();
    }
}

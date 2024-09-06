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
import org.ivoa.dm.caom2.caom2.Observation;
import org.ivoa.dm.caom2.caom2.SimpleObservation;

import java.lang.annotation.Annotation;
import java.net.URI;
import java.util.*;


//@Produces(MediaType.APPLICATION_JSON)
@Path("/")
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

    @GET
    @Path("/observations")
    @Operation(summary = "Retrieve all observations")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Observation> getAllObservations() {
        TypedQuery<Observation> query = em.createQuery("SELECT o FROM Observation o", Observation.class);
      //  query.setMaxResults(10); //TODO pagination
        return query.getResultList();
    }

    @GET
    @Path("/observations/{collection}")
    @Operation(summary = "Retrieve observations from a collection")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Observation> getObservations(@PathParam("collection") String collection) {
        TypedQuery<Observation> query = em.createQuery("SELECT o FROM Observation o WHERE o.collection = :collection", Observation.class);
        query.setParameter("collection", collection); // Set the parameter value
        return query.getResultList();
    }

    @DELETE
    @Path("/delete/{observationId}")
    @Operation(summary = "Delete an existing observation")
    @Consumes(MediaType.APPLICATION_XML)
    @Transactional
    public Response deleteObservation(@PathParam("observationId") String id) {
        Observation observation = em.find(Observation.class, id);
        if (observation != null) {
            em.remove(observation);
        }
        return Response.ok("Observation deleted successfully").build();
    }
}

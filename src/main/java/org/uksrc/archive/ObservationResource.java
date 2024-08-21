package org.uksrc.archive;
/*
 * Created on 21/08/2024 by Paul Harrison (paul.harrison@manchester.ac.uk).
 */

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.ivoa.dm.caom2.caom2.Observation;


@Produces(MediaType.APPLICATION_JSON)
@Path("/observation")
public class ObservationResource {


    @PersistenceContext
    protected EntityManager em;  // exists for the application lifetime no need to close

    @POST
    @Operation(summary = "create a new Observation")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional
    public Observation addObservation(Observation observation) {
        em.persist(observation);
        return observation;
    }

}

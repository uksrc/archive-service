package org.uksrc.archive;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.ivoa.dm.caom2.Observation;

import java.util.List;

@Path("/search")
public class ObsSearchResource {

    @PersistenceContext
    protected EntityManager em;

   public static final String CONE_SEARCH_QUERY =
           "SELECT o FROM Observation o JOIN o.targetPosition tp JOIN tp.coordinates p" +
                   " WHERE pgsphere_check_distance(p.cval1, p.cval2, :ra, :dec) <= :radiusInDegrees";

    @GET
    @Path("/cone")
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public Response getAllObservations(@QueryParam("ra") Double ra, @QueryParam("dec") Double dec, @QueryParam("radius") Double radius) {
        TypedQuery<Observation> query = em.createQuery(CONE_SEARCH_QUERY, Observation.class);
        query.setParameter("ra", ra);
        query.setParameter("dec", dec);
        query.setParameter("radiusInDegrees", radius);

        try {
            List<Observation> observations = query.getResultList();
            return Response.ok(observations).build();
        } catch (Exception e) {
            System.err.println("Query Execution Error: " + e.getMessage());
            return Response.serverError().entity("Database query failed.").build();
        }
    }
}

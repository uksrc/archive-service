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
import org.uksrc.archive.auth.ConditionalRolesAllowed;
import org.uksrc.archive.utils.responses.Responses;
import org.uksrc.archive.utils.tools.Tools;

@Path("/search")
public class ObsSearchResource {

    @PersistenceContext
    protected EntityManager em;

   public static final String CONE_SEARCH_QUERY =
           "SELECT obs FROM Observation obs JOIN obs.targetPosition tp JOIN tp.coordinates p" +
                   " WHERE FUNCTION('pgsphere_distance', p.cval1, p.cval2, :ra, :dec) <= radians(:radiusInDegrees)";

    @GET
    @Path("/cone")
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @ConditionalRolesAllowed("resource.roles.view")
    public Response getAllObservations(@QueryParam("ra") Double ra, @QueryParam("dec") Double dec, @QueryParam("radius") Double radius,
                                       @QueryParam("page") Integer page, @QueryParam("size") Integer size) {
        if ((page != null) ^ (size != null)) {
            return Responses.errorResponse("Both 'page' and 'size' must be provided together or neither.");
        } else if ((page != null && page < 0) || (size != null && size < 1)) {
            return Responses.errorResponse("Page must be 0 or greater and size must be greater than 0.");
        }

        TypedQuery<Observation> query = em.createQuery(CONE_SEARCH_QUERY, Observation.class);
        query.setParameter("ra", ra);
        query.setParameter("dec", dec);
        query.setParameter("radiusInDegrees", radius);

        try {
            return Tools.performQuery(page, size, query);
        } catch (Exception e) {
            System.err.println("Query Execution Error: " + e.getMessage());
            return Response.serverError().entity("Database query failed.").build();
        }
    }
}

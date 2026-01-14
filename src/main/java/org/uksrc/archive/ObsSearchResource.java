package org.uksrc.archive;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Query;
import org.ivoa.dm.caom2.SimpleObservation;

import java.util.List;
import java.util.stream.Collectors;

@GraphQLApi
//@Path("/search")
//@ApplicationScoped
public class ObsSearchResource {

    @PersistenceContext
    protected EntityManager em;

  /* public static final String CONE_SEARCH_QUERY =
           "SELECT obs FROM Observation obs JOIN obs.targetPosition tp JOIN tp.coordinates p" +
                   " WHERE FUNCTION('pgsphere_distance', p.cval1, p.cval2, :ra, :dec) <= radians(:radiusInDegrees)";

    @GET
    @Path("/cone")
    @Operation(summary = "Cone search of Observations", description = "Returns a list of Observations that are located within the supplied cone")
    @Parameters({
            @Parameter(
                    name = "ra",
                    description = "Right ascension in degrees",
                    in = ParameterIn.QUERY,
                    schema = @Schema(type = SchemaType.NUMBER, format = "double")
            ),
            @Parameter(
                    name = "dec",
                    description = "Declination in degrees",
                    in = ParameterIn.QUERY,
                    schema = @Schema(type = SchemaType.NUMBER, format = "double")
            ),
            @Parameter(
                    name = "radius",
                    description = "Radius in degrees",
                    in = ParameterIn.QUERY,
                    schema = @Schema(type = SchemaType.NUMBER, format = "double")
            ),
            @Parameter(
                    name = "page",
                    description = "The page number to retrieve, zero-indexed. If not provided, ALL results are returned.",
                    in = ParameterIn.QUERY,
                    schema = @Schema(type = SchemaType.INTEGER, minimum = "0")
            ),
            @Parameter(
                    name = "size",
                    description = "The number of observations per page. If not provided, ALL results are returned.",
                    in = ParameterIn.QUERY,
                    schema = @Schema(type = SchemaType.INTEGER, minimum = "1")
            )
    })
    @APIResponse(
            responseCode = "200",
            description = "List of observations retrieved successfully",
            content = {
                    @Content(
                            mediaType = MediaType.APPLICATION_XML, schema = @Schema(oneOf = ObservationListWrapper.class)
                    ),
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON, schema = @Schema(oneOf = ObservationListWrapper.class)
                    )
            }
    )
    @APIResponse(
            responseCode = "400",
            description = "If all the required parameters are not supplied"
    )
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @ConditionalRolesAllowed("resource.roles.view")
    public Response getAllObservations(@QueryParam("ra") Double ra, @QueryParam("dec") Double dec, @QueryParam("radius") Double radius,
                                       @QueryParam("page") Integer page, @QueryParam("size") Integer size) {
        if ((page != null) ^ (size != null)) {
            return Responses.errorResponse("Both 'page' and 'size' must be provided together or neither.");
        } else if ((page != null && page < 0) || (size != null && size < 1)) {
            return Responses.errorResponse("Page must be 0 or greater and size must be greater than 0.");
        }

        if (ra == null || dec == null || radius == null) {
            return Responses.errorResponse("All parameters 'ra', 'dec' and 'radius' must be supplied.");
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
    }*/

    /*@Query("observations")
    public List<Observation> getSimpleObjects() {
        return em.createQuery(
                "SELECT o FROM Observation o", Observation.class
        ).getResultList();
    }*/

    /*@Query("elites")
    public List<UpperClass> getSimpleObjects() {
        return em.createQuery(
                "SELECT o FROM UpperClass o", UpperClass.class
        ).getResultList();
    }*/


   /* @Transactional
    @Query("getElites")
    public List<EliteClass> getElites() {
        return em.createQuery("SELECT e FROM EliteClass e", EliteClass.class)
                .getResultList();
    }*/
  /* @Transactional
   @Query("getElites")
   public List<Lower> getElites() {
       return em.createQuery("SELECT e FROM UpperRes e", Upper.class)
               .getResultList()
               .stream()
               .map(e -> (Lower) e)
               .toList();
   }*/

    @Query("observations")
    public List<GqlObservation> getObservations() {
        return em.createQuery("SELECT o FROM SimpleObservation o", SimpleObservation.class)
                .getResultList()
                .stream()
                .map(GqlObservation::new)
                .collect(Collectors.toList());
    }
}

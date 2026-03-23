package org.uksrc.archive;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.ParameterIn;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameters;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.ivoa.dm.caom2.Observation;
import org.uksrc.archive.auth.ConditionalRolesAllowed;
import org.uksrc.archive.searchrequest.params.descriptors.PredicateDescriptor;
import org.uksrc.archive.searchrequest.params.parser.DescriptorFactory;
import org.uksrc.archive.searchrequest.service.ObservationSearchService;
import org.uksrc.archive.utils.ObservationListWrapper;
import org.uksrc.archive.utils.responses.Responses;
import org.uksrc.archive.utils.tools.Tools;

import java.util.List;

@Path("/search")
public class ObsSearchResource {

    @PersistenceContext
    protected EntityManager em;

    @Inject
    ObservationSearchService searchService;

    @Inject
    DescriptorFactory descriptorFactory;

   public static final String CONE_SEARCH_QUERY =
           "SELECT obs FROM Observation obs JOIN obs.targetPosition tp JOIN tp.coordinates p" +
                   " WHERE FUNCTION('pgsphere_distance', p.cval1, p.cval2, :ra, :dec) <= radians(:radiusInDegrees)";

    @GET
    @Path("/")
    @Operation(summary = "Search of Observations", description = "Returns a list of Observations that match the supplied parameter values.")
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
                    name = "project",
                    description = "Project name",
                    in = ParameterIn.QUERY,
                    schema = @Schema(type = SchemaType.STRING)
            ),
            @Parameter(
                    name = "telescope",
                    description = "Telescope name",
                    in = ParameterIn.QUERY,
                    schema = @Schema(type = SchemaType.STRING)
            ),
            @Parameter(
                    name = "instrument",
                    description = "Instrument name",
                    in = ParameterIn.QUERY,
                    schema = @Schema(type = SchemaType.STRING)
            ),
            @Parameter(
                    name = "target",
                    description = "Target name",
                    in = ParameterIn.QUERY,
                    schema = @Schema(type = SchemaType.STRING)
            ),
            @Parameter(
                    name = "band",
                    description = "Energy band",
                    in = ParameterIn.QUERY,
                    schema = @Schema(type = SchemaType.STRING)
            ),
            @Parameter(
                    name = "freqMin",
                    description = "Minimum frequency (for use in a range)",
                    in = ParameterIn.QUERY,
                    schema = @Schema(type = SchemaType.NUMBER, format = "double")
            ),
            @Parameter(
                    name = "freqMax",
                    description = "Maximum frequency (for use in a range)",
                    in = ParameterIn.QUERY,
                    schema = @Schema(type = SchemaType.NUMBER, format = "double")
            ),
            @Parameter(
                    name = "dateMin",
                    description = "Minimum date (for use in a range)",
                    in = ParameterIn.QUERY,
                    schema = @Schema(type = SchemaType.STRING, format = "ISO8601")
            ),
            @Parameter(
                    name = "dateMax",
                    description = "Maximum date (for use in a range)",
                    in = ParameterIn.QUERY,
                    schema = @Schema(type = SchemaType.STRING, format = "ISO8601")
            ),
            @Parameter(
                    name = "startDate",
                    description = "Returns any observation active beyond this date",
                    in = ParameterIn.QUERY,
                    schema = @Schema(type = SchemaType.STRING, format = "ISO8601")
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
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @ConditionalRolesAllowed("resource.roles.view")
    public Response search(@Context UriInfo uriInfo) {
        MultivaluedMap<String, String> params = uriInfo.getQueryParameters();

        try {
            String pageNum = params.getFirst("page");
            String pageSize = params.getFirst("size");
            Integer page = null;
            Integer size = null;

            if (pageNum != null && pageSize != null) {
                page = Integer.parseInt(pageNum);
                size = Integer.parseInt(pageSize);
            }

            List<PredicateDescriptor> descriptors = descriptorFactory.fromQueryParams(params);

            TypedQuery<Observation> query = searchService.searchQuery(descriptors);
            return Tools.performQuery(page, size, query);
        } catch (Exception e) {
            return Responses.errorResponse(e);
        }
    }

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
    }
}

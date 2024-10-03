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
import org.eclipse.microprofile.openapi.annotations.enums.ParameterIn;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameters;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.hibernate.PropertyValueException;
import org.ivoa.dm.caom2.caom2.DerivedObservation;
import org.ivoa.dm.caom2.caom2.Observation;
import org.ivoa.dm.caom2.caom2.SimpleObservation;
import jakarta.validation.constraints.NotNull;
import org.uksrc.archive.utils.ObservationListWrapper;

import java.util.*;


@Path("/observations")
public class ObservationResource {

    @PersistenceContext
    protected EntityManager em;  // exists for the application lifetime no need to close

    @POST
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
        return submitObservation(observation);
    }

    @POST
    @Path("/derived/")
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
        return submitObservation(observation);
    }

    @PUT
    @Path("{observationId}")
    @Operation(summary = "Update an existing Observation", description = "Updates an existing observation with the supplied ID")
    @Parameter(
            name = "observationId",
            description = "ID of the Observation to be updated",
            required = true,
            example = "123"
    )
    @RequestBody(
            description = "XML representation of the Observation",
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
                .type(MediaType.TEXT_PLAIN)
                .entity("Observation not found")
                .build();
    }

    @GET
    @Operation(summary = "Retrieve list(s) of observations", description = "Returns either all the Observations currently stored or a subset using pagination IF page AND size are supplied.")
    @Parameters({
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
            content = @Content(
                    mediaType = MediaType.APPLICATION_XML, schema = @Schema(implementation = ObservationListWrapper.class)
            )
    )
    @APIResponse(
            responseCode = "400",
            description = "Internal error whilst retrieving Observations or parameter error (if supplied)."
    )
    @Produces(MediaType.APPLICATION_XML)
    public Response getAllObservations(@QueryParam("page") Integer page, @QueryParam("size") Integer size) {
        if ((page != null && size == null) || (page == null && size != null)) {
            return errorResponse("Both 'page' and 'size' must be provided together or neither.");
        }

        try {
            if (page != null && (page < 0 || size < 1)) {
                return errorResponse("Page must be 0 or greater and size must be greater than 0.");
            }

            // Create query and apply pagination if required
            TypedQuery<Observation> query = em.createQuery("SELECT o FROM Observation o", Observation.class);
            return performQuery(page, size, query);
        } catch (Exception e) {
            return errorResponse(e);
        }
    }

    @GET
    @Path("/collection/{collectionId}")
    @Operation(summary = "Retrieve observations from a collection", description = "Returns a list of observations that are members of the supplied collection")
    @Parameters({
            @Parameter(
                    name = "collectionId",
                    description = "The collection name to retrieve observations for",
                    in = ParameterIn.PATH,
                    required = true,
                    example = "e-merlin"
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
            content = @Content(
                    mediaType = MediaType.APPLICATION_XML, schema = @Schema(implementation = ObservationListWrapper.class)
            )
    )
    @APIResponse(
            responseCode = "400",
            description = "Internal error whilst retrieving Observations."
    )
    @Produces(MediaType.APPLICATION_XML)
    public Response getObservations(@PathParam("collectionId") String collection, @QueryParam("page") Integer page, @QueryParam("size") Integer size) {
        if ((page != null && size == null) || (page == null && size != null)) {
            return errorResponse("Both 'page' and 'size' must be provided together or neither.");
        }

        try {
            if (page != null && (page < 0 || size < 1)) {
                return errorResponse("Page must be 0 or greater and size must be greater than 0.");
            }

            TypedQuery<Observation> query = em.createQuery("SELECT o FROM Observation o WHERE o.collection = :collection", Observation.class);
            query.setParameter("collection", collection);
            return performQuery(page, size, query);
        } catch (Exception e) {
            return errorResponse(e);
        }
    }

    @GET
    @Path("/{observationId}")
    @Operation(summary = "Retrieve observations from a collection", description = "Returns a list of observations that are members of the supplied collection")
    @Parameters({
            @Parameter(
                    name = "observationId",
                    description = "The id of the observation",
                    required = true,
                    example = "123456"
            )
    })
    @APIResponse(
            responseCode = "200",
            description = "Observation retrieved successfully",
            content = @Content(
                    mediaType = MediaType.APPLICATION_XML, schema = @Schema(implementation = Observation.class)
            )
    )
    @APIResponse(
            responseCode = "404",
            description = "Observation not found"
    )
    @APIResponse(
            responseCode = "400",
            description = "Internal error whilst retrieving Observations."
    )
    @Produces(MediaType.APPLICATION_XML)
    public Response getObservation(@PathParam("observationId") String observationId) {
        try {
            Observation observation = em.find(Observation.class, observationId);
            if (observation != null) {
                return Response.status(Response.Status.OK)
                        .entity(observation).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND)
                        .type(MediaType.TEXT_PLAIN)
                        .entity("Observation with ID " + observationId + " not found").build();
            }
        } catch (Exception e) {
            return errorResponse(e);
        }
    }

    @DELETE
    @Path("/{observationId}")
    @Operation(summary = "Delete an existing observation")
    @Parameters({
            @Parameter(
                    name = "observationId",
                    description = "The id of the observation to delete.",
                    required = true,
                    example = "123"
            )
    })
    @APIResponse(
            responseCode = "204",
            description = "Observation deleted."
    )
    @APIResponse(
            responseCode = "404",
            description = "Observation not found"
    )
    @APIResponse(
            responseCode = "400",
            description = "Internal error whilst deleting the observation."
    )
    @Transactional
    public Response deleteObservation(@PathParam("observationId") String id) {
        try {
            Observation observation = em.find(Observation.class, id);
            if (observation != null) {
                em.remove(observation);
                return Response.status(Response.Status.NO_CONTENT).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND)
                        .type(MediaType.TEXT_PLAIN)
                        .entity("Observation with ID " + id + " not found")
                        .build();
            }
        } catch (Exception e) {
            return errorResponse(e);
        }
    }

    @GET
    @Path("/collections")
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
                    .entity(convertListToTsv(uniqueCollections))
                    .build();
        } catch (Exception e) {
            return errorResponse(e);
        }
    }

    /**
     * Adds an observation to the database
     * @param observation Either a SimpleObservation or a DerivedObservation
     * @return Response containing status code and added observation (if successful)
     */
    private Response submitObservation(Observation observation) {
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

    /**
     * Generate an error response
     * @param e Whatever exception has been thrown
     * @return A 400 response containing the exception error.
     */
    private Response errorResponse (@NotNull Exception e){
        String additional = "";
        if (e instanceof PropertyValueException){
            //Inform caller of exact property that's missing/invalid
            additional = ((PropertyValueException)e).getPropertyName();
        }
        return Response.status(Response.Status.BAD_REQUEST)
                .type(MediaType.TEXT_PLAIN)
                .entity(e.getMessage() + " " + additional)
                .build();
    }

    /**
     * Generate an error message
     * @param message Message to return to the caller.
     * @return A 400 response containing the supplied message
     */
    private Response errorResponse (@NotNull String message){
        return Response.status(Response.Status.BAD_REQUEST)
                .type(MediaType.TEXT_PLAIN)
                .entity(message)
                .build();
    }

    /**
     * Performs the supplied query (with or without the pagination parameters)
     * @param page zero-indexed page index
     * @param size number of entries per page
     * @param query query to perform
     * @return Response containing HTTP response code and expected body if successful.
     */
    private Response performQuery(Integer page, Integer size, TypedQuery<Observation> query) {
        try {
            if (page != null && size != null) {
                int firstResult = page * size;
                query.setFirstResult(firstResult);
                query.setMaxResults(size);
            }

            List<Observation> observations = query.getResultList();
            ObservationListWrapper wrapper = new ObservationListWrapper(observations);

            return Response.ok(wrapper).build();
        } catch (Exception e) {
            return errorResponse(e);
        }
    }

    /**
     * Converts a List of strings to a TSV
     * @param list The list of elements to convert to a TSV string.
     * @return list of items "e-merlin  test    ALMA"
     */
    public String convertListToTsv(List<String> list) {
        StringJoiner joiner = new StringJoiner("\t");

        for (String item : list) {
            joiner.add(item);
        }
        return joiner.toString();
    }
}

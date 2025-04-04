package org.uksrc.archive;
/*
 * Created on 21/08/2024 by Paul Harrison (paul.harrison@manchester.ac.uk).
 */

import jakarta.persistence.*;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import org.apache.commons.beanutils.BeanUtils;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.ParameterIn;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameters;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.ivoa.dm.caom2.DerivedObservation;
import org.ivoa.dm.caom2.Observation;
import org.ivoa.dm.caom2.SimpleObservation;
import org.uksrc.archive.utils.responses.Responses;
import org.uksrc.archive.utils.tools.Tools;

@SuppressWarnings("unused")
@Path("/observations")
public class ObservationResource {

    @PersistenceContext
    protected EntityManager em;  // exists for the application lifetime no need to close

    @POST
    @Operation(summary = "Create a new Observation", description = "Creates a new observation in the database, note the supplied ID needs to be unique and XML namespace/JSON type supplied.")
    @RequestBody(
            description = "XML representation of the Observation.",
            required = true,
            content = {
                    @Content(
                            mediaType = MediaType.APPLICATION_XML,
                            schema = @Schema(oneOf = {SimpleObservation.class, DerivedObservation.class}),
                            examples = {
                                    @ExampleObject(
                                            name = "XML Example - SimpleObservation",
                                            value = """
                                                    <SimpleObservation xmlns:caom2="http://ivoa.net/dm/models/vo-dml/experiment/caom2"  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="caom2:caom2.SimpleObservation">
                                                    <collection>test</collection>
                                                    <uri>https://observatory.org/observations/CY9004_C_001_20200721</uri>
                                                    <intent>science</intent>
                                                    </SimpleObservation>"""
                                    ),
                                    @ExampleObject(
                                            name = "XML Example - DerivedObservation",
                                            value = """
                                                    <DerivedObservation xmlns:caom2="http://ivoa.net/dm/models/vo-dml/experiment/caom2"  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="caom2:caom2.DerivedObservation">
                                                    <collection>test</collection>
                                                    <uri>https://observatory.org/observations/CY9004_C_001_20200722</uri>
                                                    <intent>science</intent>
                                                    <members>jbo-simple1</members>
                                                    <members>jbo-simple2</members>
                                                    </DerivedObservation>"""
                                    )
                            }
                    ),
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(oneOf = {SimpleObservation.class, DerivedObservation.class}),
                            examples = {
                                    @ExampleObject(
                                            name = "JSON Example - SimpleObservation",
                                            value = """
                                                     {
                                                        "@type": "caom2:caom2.SimpleObservation",
                                                        "collection": "test",
                                                        "uri": "https://observatory.org/observations/CY9004_C_001_20200721",
                                                        "intent": "science",
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "JSON Example - DerivedObservation",
                                            value = """
                                                    {
                                                        "@type": "caom2:caom2.DerivedObservation",
                                                        "collection": "test",
                                                        "uri": "https://observatory.org/observations/CY9004_C_001_20200722",
                                                        "intent": "science",
                                                        "members": [
                                                            "jbo-simple1",
                                                            "jbo-simple2"
                                                        ]
                                                    }
                                                """
                                    )
                            }
                    )
            }
    )
    @APIResponse(
            responseCode = "201",
            description = "Observation created successfully",
            content = @Content(schema = @Schema(oneOf = {SimpleObservation.class, DerivedObservation.class}))
    )
    @APIResponse(
            responseCode = "400",
            description = "Invalid input"
    )
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public Response addObservation(Observation observation) {
        return submitObservation(observation);
    }

    @PUT
    @Path("/{id}")
    @Operation(summary = "Update an existing Observation", description = "Updates an existing observation with the supplied ID")
    @Parameter(
            name = "id",
            description = "ID of the Observation to be updated (UUID)",
            required = true,
            example = "c630c66f-b06b-4fed-bc16-1d7fd32161"
    )
    @RequestBody(
            description = "XML representation of the Observation",
            required = true,
            content = {
                    @Content(
                            mediaType = MediaType.APPLICATION_XML,
                            schema = @Schema(oneOf = {SimpleObservation.class, DerivedObservation.class}),
                            examples = {
                                    @ExampleObject(
                                            name = "XML Example - SimpleObservation",
                                            value = """
                                                    <SimpleObservation xmlns:caom2="http://ivoa.net/dm/models/vo-dml/experiment/caom2"  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="caom2:caom2.SimpleObservation">
                                                    <collection>test</collection>
                                                    <uri>https://observatory.org/observations/CY9004_C_001_20200721</uri>
                                                    <intent>science</intent>
                                                    </SimpleObservation>"""
                                    ),
                                    @ExampleObject(
                                            name = "XML Example - DerivedObservation",
                                            value = """
                                                    <DerivedObservation xmlns:caom2="http://ivoa.net/dm/models/vo-dml/experiment/caom2"  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="caom2:caom2.DerivedObservation">
                                                    <collection>test</collection>
                                                    <uri>https://observatory.org/observations/CY9004_C_001_20200722</uri>
                                                    <intent>science</intent>
                                                    <members>jbo-simple1</members>
                                                    <members>jbo-simple2</members>
                                                    </DerivedObservation>"""
                                    )
                            }
                    ),
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(oneOf = {SimpleObservation.class, DerivedObservation.class}),
                            examples = {
                                    @ExampleObject(
                                            name = "JSON Example - SimpleObservation",
                                            value = """
                                                     {
                                                        "@type": "caom2:caom2.SimpleObservation",
                                                        "collection": "test",
                                                        "uri": "https://observatory.org/observations/CY9004_C_001_20200721",
                                                        "intent": "science",
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "JSON Example - DerivedObservation",
                                            value = """
                                                    {
                                                        "@type": "caom2:caom2.DerivedObservation",
                                                        "collection": "test",
                                                        "uri": "https://observatory.org/observations/CY9004_C_001_20200721",
                                                        "intent": "science",
                                                        "members": [
                                                            "jbo-simple1",
                                                            "jbo-simple2"
                                                        ]
                                                    }
                                                """
                                    )
                            }
                    )
            }
    )
    @APIResponse(
            responseCode = "200",
            description = "Observation updated successfully",
            content = @Content(schema = @Schema(oneOf = {SimpleObservation.class, DerivedObservation.class}))
    )
    @APIResponse(
            responseCode = "404",
            description = "An Observation with the supplied ID has not been found."
    )
    @APIResponse(
            responseCode = "400",
            description = "Invalid input"
    )
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public Response updateObservation(@PathParam("id") String id, Observation observation) {
        try {
            if(id == null || id.isEmpty()) {
                return Responses.errorResponse("Invalid ID");
            }
            else if (!id.equals(observation.getId())){
                return Responses.errorResponse("id MUST be the same as observation.id");
            }

            //Only update IF found
            Observation existing = findObservation(id);
            if (existing != null) {
                //Copy all properties from the supplied observation over the existing observation.
                //Observation.uri MUST remain the same and won't be affected.
                BeanUtils.copyProperties(existing, observation);
                Object formattedObs = Tools.formatObservation(existing);
                return Response.ok(formattedObs).build();
            }
        } catch (Exception e) {
            return Responses.errorResponse(e);
        }

        return Response.status(Response.Status.NOT_FOUND)
                .type(MediaType.TEXT_PLAIN)
                .entity("Observation not found")
                .build();
    }

    @GET
    @Path("/")
    @Operation(summary = "Retrieve list(s) of observations", description = "Returns either all the Observations currently stored or a subset using pagination IF page AND size are supplied.")
    @Parameters({
            @Parameter(
                    name = "collectionId",
                    description = "Filter the results by a collection id if required.",
                    in = ParameterIn.QUERY,
                    schema = @Schema(type = SchemaType.STRING)
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
                            //Technically it should be ObservationListWrapper containing a list of:
                            mediaType = MediaType.APPLICATION_XML, schema = @Schema(oneOf = {SimpleObservation.class, DerivedObservation.class})
                    ),
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON, schema = @Schema(oneOf = {SimpleObservation.class, DerivedObservation.class})
                    )
            }
    )
    @APIResponse(
            responseCode = "400",
            description = "Internal error whilst retrieving Observations or parameter error (if supplied)."
    )
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public Response getAllObservations(@QueryParam("collectionId") String collection, @QueryParam("page") Integer page, @QueryParam("size") Integer size) {
        //Both page and size need to be supplied OR neither
        if ((page != null) ^ (size != null)) {
            return Responses.errorResponse("Both 'page' and 'size' must be provided together or neither.");
        } else if ((page != null && page < 0) || (size != null && size < 1)) {
            return Responses.errorResponse("Page must be 0 or greater and size must be greater than 0.");
        }

        try {
            TypedQuery<Observation> query;
            if (collection != null && !collection.isEmpty()) {
                query = em.createQuery("SELECT o FROM Observation o WHERE o.collection = :collection", Observation.class);
                query.setParameter("collection", collection);
            } else {
                query = em.createQuery("SELECT o FROM Observation o", Observation.class);
            }
            return Tools.performQuery(page, size, query);
        } catch (Exception e) {
            return Responses.errorResponse(e);
        }
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Retrieve an observation", description = "Returns an observation with the supplied ID.")
    @Parameters({
            @Parameter(
                    name = "id",
                    description = "The id of the observation (UUID)",
                    required = true,
                    example = "c630c66f-b06b-4fed-bc16-1d7fd32161"
            )
    })
    @APIResponse(
            responseCode = "200",
            description = "Observation retrieved successfully",
            content = {
                    @Content(
                            mediaType = MediaType.APPLICATION_XML, schema = @Schema(oneOf = {SimpleObservation.class, DerivedObservation.class})
                    ),
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON, schema = @Schema(oneOf = {SimpleObservation.class, DerivedObservation.class})
                    )
            }
    )
    @APIResponse(
            responseCode = "404",
            description = "Observation not found"
    )
    @APIResponse(
            responseCode = "400",
            description = "Internal error whilst retrieving Observations."
    )
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public Response getObservation(@PathParam("id") String id) {
        try {
            Observation observation = findObservation(id);
            if (observation != null) {
                Object formattedObs = Tools.formatObservation(observation);

                return Response.status(Response.Status.OK)
                        .entity(formattedObs).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND)
                        .type(MediaType.TEXT_PLAIN)
                        .entity("Observation with ID " + id + " not found").build();
            }
        } catch (Exception e) {
            return Responses.errorResponse(e);
        }
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Delete an existing observation")
    @Parameters({
            @Parameter(
                    name = "id",
                    description = "The id of the observation to delete (UUID).",
                    required = true,
                    example = "c630c66f-b06b-4fed-bc16-1d7fd32161"
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
    public Response deleteObservation(@PathParam("id") String id) {
        try {
            Observation observation = findObservation(id);
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
            return Responses.errorResponse(e);
        }
    }

    /**
     * Adds an observation to the database
     * @param observation Either a SimpleObservation or a DerivedObservation
     * @return Response containing status code and added observation (if successful)
     */
    private Response submitObservation(Observation observation) {
        if (findObservation(observation.getId()) != null) {
            return Responses.errorResponse("Observation.id " + observation.getId() + " already exists.");
        }

        try {
            em.persist(observation);
            em.flush();

            Object formattedObs = Tools.formatObservation(observation);
            return Response.status(Response.Status.CREATED)
                    .entity(formattedObs)
                    .build();

        } catch (Exception e) {
            return Responses.errorResponse(e);
        }
    }

    /**
     * Checks to see if an observation with the supplied ID already exists
     * @param id Observation.id
     * @return The observation if found, null if not
     */
    private Observation findObservation(String id) {
        TypedQuery<Observation> existsQuery = em.createQuery(
                "SELECT o FROM Observation o WHERE o.id = :id", Observation.class
        );

        try {
            existsQuery.setParameter("id", id);
            return existsQuery.getSingleResult();
        } catch (NoResultException e){
            return null;
        }
    }
}

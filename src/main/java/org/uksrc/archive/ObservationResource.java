package org.uksrc.archive;
/*
 * Created on 21/08/2024 by Paul Harrison (paul.harrison@manchester.ac.uk).
 */

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import jakarta.xml.bind.JAXBElement;
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
import org.uksrc.archive.auth.ConditionalRolesAllowed;
import org.uksrc.archive.service.ObservationService;
import org.uksrc.archive.utils.responses.Responses;
import org.uksrc.archive.utils.tools.Tools;

import javax.xml.namespace.QName;

@SuppressWarnings("unused")
@Path("/observations")
public class ObservationResource {

    @Inject
    ObservationService observationService;


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
    @ConditionalRolesAllowed("resource.roles.edit")
    @Transactional
    public Response addObservation(Observation observation) {
        return observationService.submitObservation(observation);
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
    @ConditionalRolesAllowed("resource.roles.edit")
    @Transactional
    public Response updateObservation(@PathParam("id") String id, Observation observation) {
        return observationService.updateObservation(id, observation);
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
    @ConditionalRolesAllowed("resource.roles.view")
    public Response getAllObservations(@QueryParam("collectionId") String collection, @QueryParam("page") Integer page, @QueryParam("size") Integer size) {
        return observationService.getAllObservations(collection, page, size);
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
    @ConditionalRolesAllowed("resource.roles.view")
    public Response getObservation(@PathParam("id") String id) {
        return observationService.getObservation(id);
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
    @ConditionalRolesAllowed("resource.roles.edit")
    @Transactional
    public Response deleteObservation(@PathParam("id") String id) {
        return observationService.deleteObservation(id);
    }





    /**
     * Enforces the specialization of certain Observation types.
     * Converts the name to Pascal-case suitable for XML responses.
     * @param observation The single observation to rename
     * @return A JAXBElement of either SimpleObservation or DerivedObservation
     */
  /*  private Object specialiseObservation(Observation observation) {
        Object entity = null;
        if (observation instanceof SimpleObservation) {
            entity = new JAXBElement<>(
                    new QName("SimpleObservation"), SimpleObservation.class, (SimpleObservation) observation
            );
        } else if (observation instanceof DerivedObservation) {
            entity = new JAXBElement<>(
                    new QName("DerivedObservation"), DerivedObservation.class, (DerivedObservation) observation
            );
        }
        return entity;
    }*/
}

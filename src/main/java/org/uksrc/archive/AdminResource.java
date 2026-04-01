package org.uksrc.archive;


import jakarta.annotation.security.RolesAllowed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.ivoa.dm.caom2.DerivedObservation;
import org.ivoa.dm.caom2.Observation;
import org.ivoa.dm.caom2.SimpleObservation;
import org.uksrc.archive.utils.tools.Tools;

@Path("/admin")
@RolesAllowed("admin")
public class AdminResource {

    @PersistenceContext
    protected EntityManager em;

    @GET
    public String test() {
        return "Admin utilities";
    }

    @POST
    @Path("/addObservation")
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
    public Response submitObservation(Observation observation) {
        return Tools.submitObservation(em, observation);
    }
}


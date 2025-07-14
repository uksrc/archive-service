package org.uksrc.archive.utils;

import jakarta.ws.rs.core.Response;
import jakarta.xml.bind.JAXBElement;
import org.ivoa.dm.caom2.DerivedObservation;
import org.ivoa.dm.caom2.Observation;
import org.ivoa.dm.caom2.SimpleObservation;

import javax.xml.namespace.QName;

import static io.restassured.RestAssured.given;

public class Utilities {

    //Caution with the id value if re-using.
    private static final String XML_OBSERVATION = "<caom2:Observation xmlns:caom2=\"http://www.opencadc.org/caom2/xml/v2.5\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"caom2:SimpleObservation\" caom2:id=\"%s\">" +
            "<caom2:collection>%s</caom2:collection>" +
            "<caom2:uriBucket>bucket</caom2:uriBucket>" +
            "<caom2:uri>c630c66f-b06b-4fed-bc16-1d7fd32161</caom2:uri>" +
            "<caom2:intent>science</caom2:intent>\n" +
            "</caom2:Observation>";

    public static final String INCOMPLETE_XML_OBSERVATION = "<caom2:Observation xmlns:caom2=\"http://www.opencadc.org/caom2/xml/v2.5\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"caom2:SimpleObservation\" caom2:id=\"12345\">" +
            "<caom2:collection>emerlin</caom2:collection>" +
            "<caom2:uriBucket>something</caom2:uriBucket>" +
            "<caom2:uri>c630c66f-b06b-4fed-bc16-1d7fd32172</caom2:uri>" +
         //   "<caom2:intent>science</caom2:intent>\n" + //Intentionally excluded
            "</caom2:Observation>";

    public static final String COLLECTION1 = "e-merlin";
    public static final String COLLECTION2 = "testCollection";
    public static final String OBSERVATION1 = "c630c66f-b06b-4fed-bc16-1d7fd321";
    public static final String OBSERVATION2 = "c630c66f-b06b-4fed-bc16-1d7fd321";

    // Used in testing only, must conform to the @RolesAllowed setting in the APIs
    public static final String TEST_ROLE = "prototyping-groups/mini-src";
    /**
     * Adds a SimpleObservation to the database with the supplied observationId
     * @param collectionId identifier for the collection to add this observation to.
     * @return Response of 400 for failure or 201 for created successfully.
     */
    public static Response addObservationToDatabase(String collectionId, String observationId) {
        String uniqueObservation = String.format(XML_OBSERVATION, observationId, collectionId);

        try {
            String id = given()
                    .header("Content-Type", "application/xml")
                    .body(uniqueObservation)
                    .when()
                    .post("/observations")
                    .then()
                    .statusCode(Response.Status.CREATED.getStatusCode())
                    .extract()
                    .response()
                    .body().asString();

            return Response.status(Response.Status.CREATED.getStatusCode()).entity(id).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST.getStatusCode()).build();
        }
    }
}

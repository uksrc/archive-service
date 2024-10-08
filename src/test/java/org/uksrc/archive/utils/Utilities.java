package org.uksrc.archive.utils;

import jakarta.ws.rs.core.Response;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

public class Utilities {

    //Caution with the id value if re-using.
    private static final String XML_OBSERVATION = "<SimpleObservation xmlns:caom2=\"http://ivoa.net/dm/models/vo-dml/experiment/caom2\"  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"caom2:caom2.SimpleObservation\">" +
            "<id>%s</id>" +
            "<collection>%s</collection>" +
            "<intent>science</intent>" +
            "<uri>auri</uri>" +
            "</SimpleObservation>";

    public static final String COLLECTION1 = "e-merlin";
    public static final String COLLECTION2 = "testCollection";

    /**
     * Adds a SimpleObservation to the database with the supplied observationId
     * @param observationId unique identifier for the observation
     * @param collectionId identifier for the collection to add this observation to.
     * @return Response of 400 for failure or 201 for created successfully.
     */
    public static Response addObservationToDatabase(String observationId, String collectionId) {
        String uniqueObservation = String.format(XML_OBSERVATION, observationId, collectionId);

        try {
            given()
                    .header("Content-Type", "application/xml")
                    .body(uniqueObservation)
                    .when()
                    .post("/observations")
                    .then()
                    .statusCode(Response.Status.CREATED.getStatusCode())
                    .body("simpleObservation.id", is(observationId));
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST.getStatusCode()).build();
        }
        return Response.status(Response.Status.CREATED.getStatusCode()).build();
    }
}

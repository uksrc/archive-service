package org.uksrc.archive;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test class for the Observation class
 * Requirements:
 *  Postgres DB with the CAOM (2.5) models added (as tables). Should be in place automatically via the Quarkus mechanisms.
 * Note(s):
 *  TODO: CustomObjectMapper needs to be added as it's throwning a VodmlTypeResolver error at runtime if mapping responses from the REST APIs to the VODML objects.
 *  TODO: For some reason WebApplicationException is not returning messages for some errors (400 sent buy no message, modify an example Observation but remove one of the required properties such as <intent>.
 */
@QuarkusTest
public class ObservationResourceTest {

    static final String xmlObservation = "<observation>" +
                    "<id>%s</id>" +
                    "<collection>emerlin</collection>" +
                    "<intent>science</intent>" +
                    "<uri>auri</uri>" +
                    "</observation>";

    @Inject
    EntityManager em;

    @BeforeEach
    @Transactional
    public void clearDatabase() {
        // Clear the table
        em.createQuery("DELETE FROM Observation").executeUpdate();
    }

    @Test
    public void testGettingObservations() {
        //NOTE: map to CAOM object once mapper issue is resolved.
      /*  List<Observation> observations = when()
                .get("/observations/")
                .then()
                .statusCode(200)
                .extract()
                .as(new TypeRef<>() {
                });*/
        String response = when()
                .get("/observations/")
                .then()
                .statusCode(200)
                .extract()
                .asString();
        assertEquals(response, "[]");   //List empty
    }

    @Test
    public void testAddingObservation() {
        String uniqueObservation = String.format(xmlObservation, "123");

        //As the /add operation returns the added observation, check the body of the response for valid values
        given()
                .header("Content-Type", "application/xml")
                .body(uniqueObservation)
                .when()
                .put("/observations/add")
                .then()
                .statusCode(Response.Status.CREATED.getStatusCode())
                .body("simpleObservation.id", is("123"))   // XML expectation (remove 'simpleObservation.' for JSON)
                .body("simpleObservation.intent", is("science"));
    }

    @Test
    public void testAddingDuplicateObservation() {
        String duplicateObservation = String.format(xmlObservation, "256");

        // Add 1st instance
        given()
                .header("Content-Type", "application/xml")
                .body(duplicateObservation)
                .when()
                .put("/observations/add")
                .then()
                .statusCode(Response.Status.CREATED.getStatusCode())
                .body("simpleObservation.id", is("256"));

        // An Observation with the same ID as an added resource should not be allowed.
        given()
                .header("Content-Type", "application/xml")
                .body(duplicateObservation)
                .when()
                .put("/observations/add")
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode())
                .body(containsString("duplicate key value violates unique constraint"));
    }
}

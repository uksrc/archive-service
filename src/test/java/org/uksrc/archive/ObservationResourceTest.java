package org.uksrc.archive;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsString;

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

    @Test
    public void testGettingObservations() {
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

        System.out.println(response);
        //assertTrue(observations.isEmpty());
    }

    @Test
    public void testAddingObservation() {
        String xmlObservation =
                "<observation>" +
                        "<id>123</id>" +
                        "<collection>emerlin</collection>" +
                        "<intent>science</intent>" +
                        "<uri>auri</uri>" +
                        "</observation>";

        //As the /add operation returns the added observation, check the body of the response for valid values
        given()
                .header("Content-Type", "application/xml")
                .body(xmlObservation)
                .when()
                .put("/observations/add")
                .then()
                .statusCode(Response.Status.CREATED.getStatusCode())
                .body("simpleObservation.id", is("123"))   // XML expectation (remove 'simpleObservation.' for JSON)
                .body("simpleObservation.intent", is("science"));
    }

    @Test
    public void testAddingDuplicateObservation() {
        //Missing uri property
        String xmlObservation =
                "<observation>" +
                        "<id>256</id>" +
                        "<collection>emerlin</collection>" +
                        "<intent>science</intent>" +
                        "<uri>someuri</uri>" +
                        "</observation>";

        // Add 1st instance
        given()
                .header("Content-Type", "application/xml")
                .body(xmlObservation)
                .when()
                .put("/observations/add")
                .then()
                .statusCode(Response.Status.CREATED.getStatusCode())
                .body("simpleObservation.id", is("256"));

        // An Observation with the same ID as an added resource should not be allowed.
        given()
                .header("Content-Type", "application/xml")
                .body(xmlObservation)
                .when()
                .put("/observations/add")
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode())
                .body(containsString("duplicate key value violates unique constraint"));
    }
}

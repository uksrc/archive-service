package org.uksrc.archive;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.common.mapper.TypeRef;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsString;

/**
 * Test class for the Observation class
 * Requirements:
 *  Postgres DB with the CAOM (2.5) models added (as tables). Should be in place automatically via the Quarkus mechanisms.
 */
@QuarkusTest
public class ObservationResourceTest {

    //Caution with the id value if re-using.
    private static final String XML_OBSERVATION = "<observation>" +
            "<id>%s</id>" +
            "<collection>emerlin</collection>" +
            "<intent>science</intent>" +
            "<uri>auri</uri>" +
            "</observation>";

    private static final String XML_DERIVED_OBSERVATION = "<Observation>" +
            "<id>%s</id>" +
            "<collection>emerlin</collection>" +
            "<intent>science</intent>" +
            "<uri>auri</uri>" +
            "<members>someone</members>" +
            "</Observation>";

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
        // Wrapper required for de-serialisation of List<Observation>
        ObservationListWrapper wrapper = when()
                .get("/observations/")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .extract()
                .as(new TypeRef<>() {
                });

        assert(wrapper.getObservations().isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {XML_OBSERVATION, XML_DERIVED_OBSERVATION})
    public void testAddingObservation(String observation) {
        String uniqueObservation = String.format(observation, "123");

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
        String duplicateObservation = String.format(XML_DERIVED_OBSERVATION, "256");

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

    @Test
    public void testUpdatingObservation() {
        final String ID = "123";
        String uniqueObservation = String.format(XML_OBSERVATION, ID);

        // Add an observation
        given()
                .header("Content-Type", "application/xml")
                .body(uniqueObservation)
                .when()
                .put("/observations/add")
                .then()
                .statusCode(Response.Status.CREATED.getStatusCode())
                .body("simpleObservation.id", is(ID))
                .body("simpleObservation.intent", is("science"));

        // Update it with a different value
        String updatedObservation = uniqueObservation.replace("science", "calibration");
        given()
                .header("Content-Type", "application/xml")
                .body(updatedObservation)
                .when()
                .post(("/observations/update/" + ID))
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .body("simpleObservation.id", is(ID))
                .body("simpleObservation.intent", is("calibration"));

        // For completeness, we need to check that the actual entry is updated
        given()
                .header("Content-Type", "application/xml")
                .body(uniqueObservation)
                .when()
                .get("/observations/observation/" + ID)
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .body("simpleObservation.id", is(ID))   // XML expectation (remove 'simpleObservation.' for JSON)
                .body("simpleObservation.intent", is("calibration"));
    }
}

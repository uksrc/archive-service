package org.uksrc.archive;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.common.mapper.TypeRef;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
            "<collection>e-merlin</collection>" +
            "<intent>science</intent>" +
            "<uri>auri</uri>" +
            "</observation>";

    private static final String XML_DERIVED_OBSERVATION = "<Observation>" +
            "<id>%s</id>" +
            "<collection>e-merlin</collection>" +
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
    @DisplayName("Check that and empty database returns a robust response.")
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

    @Test
    @DisplayName("Add two observation and check two are returned.")
    public void testGettingObservationsNonEmpty() {
        try(Response res1 = addObservationToDatabase("1234");
            Response res2 = addObservationToDatabase("6789")) {
            assert (res1.getStatus() == Response.Status.CREATED.getStatusCode() &&
                    res2.getStatus() == Response.Status.CREATED.getStatusCode());

            ObservationListWrapper wrapper = when()
                    .get("/observations/")
                    .then()
                    .statusCode(Response.Status.OK.getStatusCode())
                    .extract()
                    .as(new TypeRef<>() {
                    });

            assert (wrapper.getObservations().size() == 2);
        }
    }

    @ParameterizedTest
    @DisplayName("Add an observation and check that part of the response body matches.")
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
    @DisplayName("Check that an error is raised if trying to add two observations with the same ID.")
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
    @DisplayName("Attempt to add some data that doesn't comply with model.")
    public void testAddingJunkObservation() {
        final String junkData = "doesn't conform with XML model for Observation";

        given()
                .header("Content-Type", "application/xml")
                .body(junkData)
                .when()
                .put("/observations/add")
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    @DisplayName("Attempt to add an Observation with a MUST property missing.")
    public void testAddingIncompleteObservation() {
        final String INCOMPLETE_XML_OBSERVATION = "<observation>" +
                "<id>444</id>" +
                "<collection>e-merlin</collection>" +
             //   "<intent>science</intent>" +      //deliberately excluded
                "<uri>auri</uri>" +
                "</observation>";

        given()
                .header("Content-Type", "application/xml")
                .body(INCOMPLETE_XML_OBSERVATION)
                .when()
                .put("/observations/add")
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    @DisplayName("Add an observation, update one of its values and update, check it's been updated correctly.")
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

    @Test
    @DisplayName("Attempt to update a non-existent observation and check the not found status.")
    public void testUpdatingNonExistingObservation() {
        final String ID = "1234";

        String obs1 = String.format(XML_OBSERVATION, ID);
        String updatedObservation = obs1.replace("science", "calibration");

        given()
                .header("Content-Type", "application/xml")
                .body(updatedObservation)
                .when()
                .post(("/observations/update/" + ID))
                .then()
                .statusCode(Response.Status.NOT_FOUND.getStatusCode());
    }

    @Test
    @DisplayName("Attempt to delete an observation.")
    public void testDeletingObservation() {
        final String ID = "256";
        try(Response res = addObservationToDatabase(ID)) {
            assert (res.getStatus() == Response.Status.CREATED.getStatusCode());

            // Check it exists
            given()
                    .header("Content-Type", "application/xml")
                    .when()
                    .get("/observations/observation/" + ID)
                    .then()
                    .statusCode(Response.Status.OK.getStatusCode())
                    .body("simpleObservation.id", is(ID));

            given()
                    .header("Content-Type", "application/xml")
                    .when()
                    .delete(("/observations/delete/" + ID))
                    .then()
                    .statusCode(Response.Status.NO_CONTENT.getStatusCode());
        }
    }

    @Test
    @DisplayName("Attempt to delete an observation that doesn't exist.")
    public void testDeletingNonExistingObservation() {
        given()
                .header("Content-Type", "application/xml")
                .when()
                .delete(("/observations/delete/" + "9876"))
                .then()
                .statusCode(Response.Status.NOT_FOUND.getStatusCode());
    }

    /**
     * Adds a SimpleObservation to the database with the supplied observationId
     * @param observationId unique identifier for the observation
     * @return Response of 400 for failure or 201 for created successfully.
     */
    private Response addObservationToDatabase(String observationId) {
        String uniqueObservation = String.format(XML_OBSERVATION, observationId);

        try {
            given()
                    .header("Content-Type", "application/xml")
                    .body(uniqueObservation)
                    .when()
                    .put("/observations/add")
                    .then()
                    .statusCode(Response.Status.CREATED.getStatusCode())
                    .body("simpleObservation.id", is(observationId));
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST.getStatusCode()).build();
        }
        return Response.status(Response.Status.CREATED.getStatusCode()).build();
    }
}

package org.uksrc.archive;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.common.mapper.TypeRef;
import io.restassured.path.xml.XmlPath;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.uksrc.archive.utils.ObservationListWrapper;
import org.uksrc.archive.utils.Utilities;
import org.uksrc.archive.utils.responses.ObservationResponse;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.CoreMatchers.is;

/**
 * Test class for the Observation class
 * Requirements:
 *  Postgres DB with the CAOM (2.5) models added (as tables). Should be in place automatically via the Quarkus mechanisms.
 */
@QuarkusTest
public class ObservationResourceTest {

    //Caution with the id value if re-using.
    private static final String XML_OBSERVATION = "<SimpleObservation xmlns:caom2=\"http://ivoa.net/dm/models/vo-dml/experiment/caom2\"  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"caom2:SimpleObservation\">" +
            "<collection>%s</collection>" +
            "<intent>science</intent>" +
            "<uri>auri</uri>" +
            "</SimpleObservation>";

    private static final String XML_DERIVED_OBSERVATION = "<DerivedObservation xmlns:caom2=\"http://ivoa.net/dm/models/vo-dml/experiment/caom2\"  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"caom2:DerivedObservation\">" +
            "<collection>e-merlin</collection>" +
            "<intent>science</intent>" +
            "<uri>auri</uri>" +
            "<members>someone</members>" +
            "</DerivedObservation>";

    private static final String COLLECTION1 = "e-merlin";
    private static final String COLLECTION2 = "testCollection";

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
        try(Response res1 = Utilities.addObservationToDatabase(COLLECTION1);
            Response res2 = Utilities.addObservationToDatabase(COLLECTION1)) {
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

    @Test
    @DisplayName("Get observations via collection Id")
    public void testGettingObservationsViaCollectionId() {
        try(Response res1 = Utilities.addObservationToDatabase(COLLECTION1);
            Response res2 = Utilities.addObservationToDatabase(COLLECTION1)) {
            assert (res1.getStatus() == Response.Status.CREATED.getStatusCode() &&
                    res2.getStatus() == Response.Status.CREATED.getStatusCode());

            //Both previously added observations should be returned
            ObservationListWrapper wrapper = when()
                    .get("/observations?collectionId=" + COLLECTION1)
                    .then()
                    .statusCode(Response.Status.OK.getStatusCode())
                    .extract()
                    .as(new TypeRef<>() {
                    });

            assert (wrapper.getObservations().size() == 2);

            //Neither of the previously added observations should be returned
            wrapper = when()
                    .get("/observations?collectionId=" + COLLECTION2)
                    .then()
                    .statusCode(Response.Status.OK.getStatusCode())
                    .extract()
                    .as(new TypeRef<>() {
                    });

            assert (wrapper.getObservations().isEmpty());
        }
    }

    @ParameterizedTest
    @DisplayName("Add an observation and check that part of the response body matches.")
    @ValueSource(strings = {XML_OBSERVATION, XML_DERIVED_OBSERVATION})
    public void testAddingObservation(String observation) {
        String uniqueObservation = String.format(observation, COLLECTION1);

        //As the /add operation returns the added observation, check the body of the response for valid values
        String res = given()
                .header("Content-Type", "application/xml")
                .body(uniqueObservation)
                .when()
                .post("/observations")
                .then()
                .statusCode(Response.Status.CREATED.getStatusCode())// XML expectation (remove 'simpleObservation.' for JSON)
                .extract().response().body().asString();

        String searchString = "ObservationResponse.SimpleObservation.";
        if (observation.contains("DerivedObservation")){
            searchString = "ObservationResponse.DerivedObservation.";
        }
        String intent = XmlPath.from(res).getString(searchString + "intent");
        String uri = XmlPath.from(res).getString(searchString + "uri");

        assert(intent.compareTo("science") == 0);
        assert(uri.compareTo("auri") == 0);
    }

    //Disabled as ID is now auto-generated in the database.
    //Leaving in case we want to add a canonical ID too (or revert back to supplying an ID)
/*    @Test
    @DisplayName("Check that an error is raised if trying to add two observations with the same ID.")
    public void testAddingDuplicateObservation() {
        String duplicateObservation = String.format(XML_DERIVED_OBSERVATION, "256");

        // Add 1st instance
        given()
                .header("Content-Type", "application/xml")
                .body(duplicateObservation)
                .when()
                .post("/observations")
                .then()
                .statusCode(Response.Status.CREATED.getStatusCode())
                .body("simpleObservation.id", is("256"));

        // An Observation with the same ID as an added resource should not be allowed.
        given()
                .header("Content-Type", "application/xml")
                .body(duplicateObservation)
                .when()
                .post("/observations")
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode())
                .body(containsString("duplicate key value violates unique constraint"));
    }*/

    @Test
    @DisplayName("Attempt to add some data that doesn't comply with model.")
    public void testAddingJunkObservation() {
        final String junkData = "doesn't conform with XML model for Observation";

        given()
                .header("Content-Type", "application/xml")
                .body(junkData)
                .when()
                .post("/observations/add")
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
                .post("/observations/add")
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    @DisplayName("Add an observation, update one of its values and update, check it's been updated correctly.")
    public void testUpdatingObservation() {
        String uniqueObservation = String.format(XML_OBSERVATION, COLLECTION1);

        // Add an observation
        ObservationResponse res = given()
                .header("Content-Type", "application/xml")
                .body(uniqueObservation)
                .when()
                .post("/observations")
                .then()
                .statusCode(Response.Status.CREATED.getStatusCode())
                .extract()
                .as(new TypeRef<>() {});
        final String ID = String.valueOf(res.getId());

        // Update it with a different value
        String updatedObservation = uniqueObservation.replace("science", "calibration");
        given()
                .header("Content-Type", "application/xml")
                .body(updatedObservation)
                .when()
                .put(("/observations/" + ID))
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .body("simpleObservation.id", is(ID))
                .body("simpleObservation.SimpleObservation.intent", is("calibration"));

        // For completeness, we need to check that the actual entry is updated
        given()
                .header("Content-Type", "application/xml")
                .body(uniqueObservation)
                .when()
                .get("/observations/" + ID)
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .body("simpleObservation.id", is(ID))   // XML expectation (remove 'simpleObservation.' for JSON)
                .body("simpleObservation.SimpleObservation.intent", is("calibration"));
    }

    @Test
    @DisplayName("Attempt to update a non-existent observation and check the not found status.")
    public void testUpdatingNonExistingObservation() {
        final String ID = "1";

        String obs1 = String.format(XML_OBSERVATION, COLLECTION1);
        String updatedObservation = obs1.replace("science", "calibration");

        given()
                .header("Content-Type", "application/xml")
                .body(updatedObservation)
                .when()
                .put(("/observations/" + ID))
                .then()
                .statusCode(Response.Status.NOT_FOUND.getStatusCode());
    }

    @Test
    @DisplayName("Attempt to delete an observation.")
    public void testDeletingObservation() {
        String id;
        try(Response res = Utilities.addObservationToDatabase(COLLECTION1)) {
            assert (res.getStatus() == Response.Status.CREATED.getStatusCode());
            String resBody = (String) res.getEntity();
            id = XmlPath.from(resBody).getString("SimpleObservation.id");

            // Check it exists
            given()
                    .header("Content-Type", "application/xml")
                    .when()
                    .get("/observations/" + id)
                    .then()
                    .statusCode(Response.Status.OK.getStatusCode())
                    .body("simpleObservation.id", is(id));

            given()
                    .header("Content-Type", "application/xml")
                    .when()
                    .delete(("/observations/" + id))
                    .then()
                    .statusCode(Response.Status.NO_CONTENT.getStatusCode());
        }
    }

    @Test
    @DisplayName("Test paging results, first page")
    public void testPagingResults() {
        for (int i = 0; i < 15; i++){
            Utilities.addObservationToDatabase(COLLECTION1);
        }

        ObservationListWrapper wrapper = when()
                .get("/observations?page=0&size=10")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .extract()
                .as(new TypeRef<>() {
                });

        assert(wrapper.getObservations().size() == 10);
    }

    @Test
    @DisplayName("Test paging results, second page")
    public void testPagingResults2() {
        for (int i = 0; i < 15; i++){
            Utilities.addObservationToDatabase(COLLECTION1);
        }

        ObservationListWrapper wrapper = when()
                .get("/observations?page=1&size=10")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .extract()
                .as(new TypeRef<>() {
                });

        //As 15 were added, only five should be returned for the second page (0-indexed)
        final int size = wrapper.getObservations().size();
        assert(size == 5);

        //Ensure that the returned 5 are actually the last five
        ObservationResponse lastEntry = wrapper.getObservations().get(size - 1);
        System.out.println(lastEntry.getId());
        assert (lastEntry.getId() >= 15);
    }

    @Test
    @DisplayName("Attempt to delete an observation that doesn't exist.")
    public void testDeletingNonExistingObservation() {
        given()
                .header("Content-Type", "application/xml")
                .when()
                .delete(("/observations/" + "9876"))
                .then()
                .statusCode(Response.Status.NOT_FOUND.getStatusCode());
    }
}

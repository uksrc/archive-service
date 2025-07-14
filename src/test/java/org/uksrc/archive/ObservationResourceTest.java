package org.uksrc.archive;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.common.mapper.TypeRef;
import io.restassured.path.xml.XmlPath;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;
import org.ivoa.dm.caom2.Observation;
import org.ivoa.dm.caom2.ObservationIntentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.uksrc.archive.utils.ObservationListWrapper;
import org.uksrc.archive.utils.Utilities;

import java.io.StringReader;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.CoreMatchers.is;
import static org.uksrc.archive.utils.Utilities.TEST_ROLE;

/**
 * Test class for the Observation class
 * Requirements:
 *  Postgres DB with the CAOM (2.5) models added (as tables). Should be in place automatically via the Quarkus mechanisms.
 */
@QuarkusTest
public class ObservationResourceTest {

    //Caution with the id value if re-using.
    private static final String XML_OBSERVATION = "<caom2:Observation xmlns:caom2=\"http://www.opencadc.org/caom2/xml/v2.5\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"caom2:SimpleObservation\" caom2:id=\"%s\">" +
            "<caom2:collection>%s</caom2:collection>" +
            "<caom2:uriBucket>bucket</caom2:uriBucket>" +
            "<caom2:uri>c630c66f-b06b-4fed-bc16-1d7fd32172</caom2:uri>" +
            "<caom2:intent>science</caom2:intent>\n" +
            "</caom2:Observation>";

    private static final String XML_DERIVED_OBSERVATION = "<caom2:Observation xmlns:caom2=\"http://www.opencadc.org/caom2/xml/v2.5\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"caom2:DerivedObservation\" caom2:id=\"%s\">" +
            "<caom2:collection>%s</caom2:collection>" +
            "<caom2:uriBucket>bucket</caom2:uriBucket>" +
            "<caom2:uri>c630c66f-b06b-4fed-bc16-1d7fd32172</caom2:uri>" +
            "<caom2:intent>science</caom2:intent>" +
            "<caom2:members>" +
            "    <caom2:member>someone</caom2:member>" +
            "</caom2:members>" +
            "</caom2:Observation>";

    private static final String COLLECTION1 = "e-merlin";
    private static final String COLLECTION2 = "testCollection";
    private static final String OBSERVATION1 = URLEncoder.encode("c630c66f-b06b-4fed-bc16-1d7fd32161", StandardCharsets.UTF_8);
    private static final String OBSERVATION2 = URLEncoder.encode("c630c66f-b06b-4fed-bc16-1d7fd32172", StandardCharsets.UTF_8);

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
    @TestSecurity(user = "testuser", roles = {TEST_ROLE})
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
    @TestSecurity(user = "testuser", roles = {TEST_ROLE})
    public void testGettingObservationsNonEmpty() {
        try(Response res1 = Utilities.addObservationToDatabase(COLLECTION1, OBSERVATION1);
            Response res2 = Utilities.addObservationToDatabase(COLLECTION1, OBSERVATION2)) {
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
    @TestSecurity(user = "testuser", roles = {TEST_ROLE})
    public void testGettingObservationsViaCollectionId() {
        try(Response res1 = Utilities.addObservationToDatabase(COLLECTION1, OBSERVATION1);
            Response res2 = Utilities.addObservationToDatabase(COLLECTION1, OBSERVATION2)) {
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
    @TestSecurity(user = "testuser", roles = {TEST_ROLE})
    @ValueSource(strings = {XML_OBSERVATION, XML_DERIVED_OBSERVATION})
    public void testAddingObservation(String observation) {
        //As the method enters twice we need to enforce different observation IDs.
        String obsId = observation.contains("DerivedObservation") ? OBSERVATION1 : OBSERVATION2;

        String uniqueObservation = String.format(observation, obsId, COLLECTION1);

        //As the /add operation returns the added observation, check the body of the response for valid values
        String res = given()
                .header("Content-Type", "application/xml")
                .body(uniqueObservation)
                .when()
                .post("/observations")
                .then()
                .statusCode(Response.Status.CREATED.getStatusCode())// XML expectation (remove 'simpleObservation.' for JSON)
                .extract().response().body().asString();

        assert(res.contains("<caom2:intent>science</caom2:intent>"));
        assert(res.contains("caom2:id=\"" + obsId + "\""));
    }

    @Test
    @DisplayName("Attempt to add some data that doesn't comply with model.")
    @TestSecurity(user = "testuser", roles = {TEST_ROLE})
    public void testAddingJunkObservation() {
        final String junkData = "doesn't conform with XML model for Observation";

        given()
                .header("Content-Type", "application/xml")
                .body(junkData)
                .when()
                .post("/observations")
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    @DisplayName("Attempt to add an Observation with a MUST property missing.")
    @TestSecurity(user = "testuser", roles = {TEST_ROLE})
    public void testAddingIncompleteObservation() {
        final String INCOMPLETE_XML_OBSERVATION = "<observation>" +
                "<id>444</id>" +
                "<collection>e-merlin</collection>" +
             //   "<intent>science</intent>" +      //deliberately excluded
                "<uri>auri</uri>" +
                "</observation>";

        given()
                .header("Content-Type", "application/xml")
                .body(Utilities.INCOMPLETE_XML_OBSERVATION)
                .when()
                .post("/observations")
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    @DisplayName("Add an observation, update one of its values and update, check it's been updated correctly.")
    @TestSecurity(user = "testuser", roles = {TEST_ROLE})
    public void testUpdatingObservation() {
        String uniqueObservation = String.format(XML_OBSERVATION, OBSERVATION2, COLLECTION1);

        // Add an observation
        given()
                .header("Content-Type", "application/xml")
                .body(uniqueObservation)
                .when()
                .post("/observations")
                .then()
                .statusCode(Response.Status.CREATED.getStatusCode())
                .extract()
                .as(new TypeRef<>() {});

        // Update it with a different value
        String updatedObservation = uniqueObservation.replace("science", "calibration");
        given()
                .header("Content-Type", "application/xml")
                .body(updatedObservation)
                .when()
                .put(("/observations/" + OBSERVATION2))
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .body("simpleObservation.uri", is(OBSERVATION2))
                .body("simpleObservation.intent", is("calibration"));

        // For completeness, we need to check that the actual entry is updated
        given()
                .header("Content-Type", "application/xml")
                .body(uniqueObservation)
                .when()
                .get("/observations/" + OBSERVATION2)
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .body("simpleObservation.uri", is(OBSERVATION2))   // XML expectation (remove 'simpleObservation.' for JSON)
                .body("simpleObservation.intent", is("calibration"));
    }

    @Test
    @DisplayName("Attempt to update a non-existent observation and check the not found status.")
    @TestSecurity(user = "testuser", roles = {TEST_ROLE})
    public void testUpdatingNonExistingObservation() {
        String obs1 = String.format(XML_OBSERVATION, OBSERVATION1, COLLECTION1);
        String updatedObservation = obs1.replace("science", "calibration");

        given()
                .header("Content-Type", "application/xml")
                .body(updatedObservation)
                .when()
                .put("/observations/" + OBSERVATION1)
                .then()
                .statusCode(Response.Status.NOT_FOUND.getStatusCode());
    }

    @Test
    @DisplayName("Attempt to delete an observation.")
    @TestSecurity(user = "testuser", roles = {TEST_ROLE})
    public void testDeletingObservation() {
        try(Response res = Utilities.addObservationToDatabase(COLLECTION1, OBSERVATION1)) {
            assert (res.getStatus() == Response.Status.CREATED.getStatusCode());

            // Check it exists
            given()
                    .header("Content-Type", "application/xml")
                    .when()
                    .get("/observations/" + OBSERVATION1)
                    .then()
                    .statusCode(Response.Status.OK.getStatusCode())
                    .body("simpleObservation.uri", is(OBSERVATION1));

            given()
                    .header("Content-Type", "application/xml")
                    .when()
                    .delete(("/observations/" + OBSERVATION1))
                    .then()
                    .statusCode(Response.Status.NO_CONTENT.getStatusCode());
        }
    }

    @Test
    @DisplayName("Test paging results, first page")
    @TestSecurity(user = "testuser", roles = {TEST_ROLE})
    public void testPagingResults() {
        for (int i = 0; i < 15; i++){
            try (Response res = Utilities.addObservationToDatabase(COLLECTION1, "observation" + i)){
                assert (res.getStatus() == Response.Status.CREATED.getStatusCode());
            }
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
    @TestSecurity(user = "testuser", roles = {TEST_ROLE})
    public void testPagingResults2() {
        final int TOTAL = 15;
        for (int i = 0; i < TOTAL; i++){
            try (Response res =Utilities.addObservationToDatabase(COLLECTION1, "observation" + i)){
                assert (res.getStatus() == Response.Status.CREATED.getStatusCode());
            }
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
    }

    @Test
    @DisplayName("Attempt to delete an observation that doesn't exist.")
    @TestSecurity(user = "testuser", roles = {TEST_ROLE})
    public void testDeletingNonExistingObservation() {
        given()
                .header("Content-Type", "application/xml")
                .when()
                .delete(("/observations/" + "9876"))
                .then()
                .statusCode(Response.Status.NOT_FOUND.getStatusCode());
    }
}

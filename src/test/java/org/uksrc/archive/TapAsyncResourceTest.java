package org.uksrc.archive;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.ivoa.dm.caom2.Observation;
import org.junit.jupiter.api.*;
import org.uksrc.archive.utils.Utilities;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.uksrc.archive.utils.Utilities.*;

/**
 * Test class for TapAsyncResource
 * Tests asynchronous TAP (Table Access Protocol) query operations.
 */
@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TapAsyncResourceTest {

    @Inject
    EntityManager em;

    @Inject
    ObservationResource observationResource;

    @BeforeEach
    @AfterAll
    @Transactional
    public void clearDatabase() {
        // Clear the table(s)
        em.createQuery("DELETE FROM Artifact").executeUpdate();
        em.createQuery("DELETE FROM Plane").executeUpdate();
        em.createQuery("DELETE FROM Observation").executeUpdate();
    }

    @Test
    @Order(1)
    @DisplayName("Test async TAP query - create job with simple ADQL query")
    @TestSecurity(user = TEST_USER, roles = {TEST_READER_ROLE, TEST_WRITER_ROLE})
    public void testCreateAsyncJob() throws Exception {
        // Add test observation data
        Observation obs1 = Utilities.createSimpleObservation(OBSERVATION1, COLLECTION1);
        observationResource.addObservation(obs1);

        // Create async TAP job with ADQL query
        String adqlQuery = "SELECT TOP 5 observationID, collection FROM ivoa.ObsCore";

        String jobLocation = given()
                .contentType(ContentType.URLENC)
                .formParam("LANG", "ADQL")
                .formParam("QUERY", adqlQuery)
                .when()
                .post("/tap/async")
                .then()
                .statusCode(303) // Expected redirect status
                .header("Location", notNullValue())
                .extract()
                .header("Location");

        // Verify job was created
        Assertions.assertNotNull(jobLocation);
        Assertions.assertTrue(jobLocation.contains("/async/"));
    }

    @Test
    @Order(2)
    @DisplayName("Test async TAP query - submit job and check execution phase")
    @TestSecurity(user = TEST_USER, roles = {TEST_READER_ROLE, TEST_WRITER_ROLE})
    public void testSubmitAsyncJobAndCheckPhase() throws Exception {
        // Add test observation data
        Observation obs1 = Utilities.createSimpleObservation(OBSERVATION1, COLLECTION1);
        Observation obs2 = Utilities.createSimpleObservation(OBSERVATION2, COLLECTION1);
        observationResource.addObservation(obs1);
        observationResource.addObservation(obs2);

        // Create async TAP job
        String adqlQuery = "SELECT observationID, collection FROM ivoa.ObsCore WHERE collection = '" + COLLECTION1 + "'";

        String jobLocation = given()
                .contentType(ContentType.URLENC)
                .formParam("LANG", "ADQL")
                .formParam("QUERY", adqlQuery)
                .when()
                .post("/tap/async")
                .then()
                .statusCode(303)
                .extract()
                .header("Location");

        String jobId = jobLocation.substring(jobLocation.lastIndexOf('/') + 1);

        // Start the job
        given()
                .contentType(ContentType.URLENC)
                .formParam("PHASE", "RUN")
                .when()
                .post("/tap/async/" + jobId + "/phase")
                .then()
                .statusCode(anyOf(is(200), is(303)));

        // Wait for job to complete (with timeout)
        int maxAttempts = 10;
        int attempt = 0;
        String phase = "PENDING";

        while (attempt < maxAttempts && !phase.equals("COMPLETED") && !phase.equals("ERROR")) {
            Thread.sleep(1000); // Wait 1 second between checks

            phase = given()
                    .when()
                    .get("/tap/async/" + jobId + "/phase")
                    .then()
                    .statusCode(200)
                    .extract()
                    .asString();

            attempt++;
        }

        // Verify job completed successfully
        Assertions.assertEquals("COMPLETED", phase, "Job should have completed successfully");

        // Retrieve results
        given()
                .when()
                .get("/tap/async/" + jobId + "/results/result")
                .then()
                .statusCode(200)
                .contentType(anyOf(containsString("votable"), containsString("xml")));
    }

    @Test
    @Order(3)
    @DisplayName("Test async TAP query - verify query results content")
    @TestSecurity(user = TEST_USER, roles = {TEST_READER_ROLE, TEST_WRITER_ROLE})
    public void testAsyncQueryResultsContent() throws Exception {
        // Add test observations with known data
        Observation obs1 = Utilities.createSimpleObservation(OBSERVATION1, COLLECTION1);
        Observation obs2 = Utilities.createSimpleObservation(OBSERVATION2, COLLECTION1);
        observationResource.addObservation(obs1);
        observationResource.addObservation(obs2);

        // Create and execute async TAP job
        String adqlQuery = "SELECT observationID FROM ivoa.ObsCore WHERE collection = '" + COLLECTION1 + "'";

        String jobLocation = given()
                .contentType(ContentType.URLENC)
                .formParam("LANG", "ADQL")
                .formParam("QUERY", adqlQuery)
                .when()
                .post("/tap/async")
                .then()
                .statusCode(303)
                .extract()
                .header("Location");

        String jobId = jobLocation.substring(jobLocation.lastIndexOf('/') + 1);

        // Start the job
        given()
                .contentType(ContentType.URLENC)
                .formParam("PHASE", "RUN")
                .when()
                .post("/tap/async/" + jobId + "/phase")
                .then()
                .statusCode(anyOf(is(200), is(303)));

        // Wait for job completion
        int maxAttempts = 10;
        int attempt = 0;
        String phase = "PENDING";

        while (attempt < maxAttempts && !phase.equals("COMPLETED") && !phase.equals("ERROR")) {
            Thread.sleep(1000);

            phase = given()
                    .when()
                    .get("/tap/async/" + jobId + "/phase")
                    .then()
                    .statusCode(200)
                    .extract()
                    .asString();

            attempt++;
        }

        Assertions.assertEquals("COMPLETED", phase, "Job should have completed");

        // Retrieve and verify results contain our observation IDs
        String results = given()
                .when()
                .get("/tap/async/" + jobId + "/results/result")
                .then()
                .statusCode(200)
                .extract()
                .asString();

        // Results should be in VOTable format and contain observation data
        Assertions.assertTrue(results.contains("VOTABLE") || results.contains("votable"),
                "Results should be in VOTable format");
        Assertions.assertTrue(results.contains(OBSERVATION1) || results.contains(OBSERVATION2),
                "Results should contain at least one of our test observations");
    }

    @Test
    @Order(4)
    @DisplayName("Test async TAP query - delete job")
    @TestSecurity(user = TEST_USER, roles = {TEST_READER_ROLE, TEST_WRITER_ROLE})
    public void testDeleteAsyncJob() throws Exception {
        // Create async TAP job
        String adqlQuery = "SELECT TOP 1 observationID FROM ivoa.ObsCore";

        String jobLocation = given()
                .contentType(ContentType.URLENC)
                .formParam("LANG", "ADQL")
                .formParam("QUERY", adqlQuery)
                .when()
                .post("/tap/async")
                .then()
                .statusCode(303)
                .extract()
                .header("Location");

        String jobId = jobLocation.substring(jobLocation.lastIndexOf('/') + 1);

        // Verify job exists
        given()
                .when()
                .get("/tap/async/" + jobId)
                .then()
                .statusCode(200);

        // Delete the job
        given()
                .when()
                .delete("/tap/async/" + jobId)
                .then()
                .statusCode(anyOf(is(200), is(303)));

        // Verify job no longer exists (should return 404)
        given()
                .when()
                .get("/tap/async/" + jobId)
                .then()
                .statusCode(404);
    }
}

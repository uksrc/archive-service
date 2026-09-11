package org.uksrc.archive;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.filter.log.LogDetail;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.Unmarshaller;
import org.ivoa.dm.caom2.DerivedObservation;
import org.ivoa.dm.caom2.Observation;
import org.junit.jupiter.api.*;
import org.uksrc.archive.utils.Utilities;

import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.uksrc.archive.utils.Utilities.*;

/**
 * Test class for TapAsyncResource
 * Tests asynchronous TAP (Table Access Protocol) query operations.
 */
@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Disabled
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TapAsyncResourceTest {

    @Inject
    EntityManager em;

    @Inject
    ObservationResource observationResource;

    private String jobId;

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
    public void testCreateAsyncJob() {
        Response createResponse = given()
                .formParam("QUERY", "select * from TAP_SCHEMA.tables")
                .redirects().follow(false)
                .when().post("/tap/async")
                .then()
                .statusCode(303)
                .header("Location", notNullValue())
                .extract().response();
        String jobFullUrl = createResponse.getHeader("Location");
        jobId = jobFullUrl.substring(jobFullUrl.lastIndexOf('/') + 1);
        assertDoesNotThrow(() -> UUID.fromString(jobId));
    }

    @Test
    @Order(2)
    @DisplayName("Check the phase")
    public void checkPhase() {
        String jobUrl = "/tap/async/" + jobId;
        //Verify Initial State (PENDING)
        given()
                .when().get(jobUrl)
                .then()
                .statusCode(200)
                .body("job.phase", equalTo("PENDING"));

        //Check the phase via the /phase api directly
        // ~/tap/sync/<jobid>/phase
        given()
                .when().get(jobUrl + "/phase")
                .then()
                .statusCode(200)
                .body(comparesEqualTo("PENDING"));
    }

    @Test
    @Order(3)
    @DisplayName("Check the parameters")
    public void checkParameters() {
        String jobUrl = "/tap/async/" + jobId;

        given()
                .when().get(jobUrl + "/parameters")
                .then()
                .statusCode(200)
                .body("parameters.parameter.size()", greaterThan(0))
                .body("parameters.parameter.find { it.@id == 'QUERY' }", equalTo("select * from TAP_SCHEMA.tables"));
    }

    @Test
    @Order(4)
    @DisplayName("Check the execution duration")
    public void checkExecutionDuration() {
        String jobUrl = "/tap/async/" + jobId;

        given()
                .when().get(jobUrl + "/executionduration")
                .then()
                .statusCode(200)
                .body(comparesEqualTo("0"));
    }

    @Test
    @Order(5)
    @DisplayName("Start the job and check the phase")
    public void checkResult() {
        String jobUrl = "/tap/async/" + jobId;

        given()
                .contentType(ContentType.URLENC)
                .redirects().follow(false)
                .formParam("PHASE", "RUN")
                .when()
                .post(jobUrl + "/phase")
                .then()
                .statusCode(303);

        // Poll until
        await().atMost(Duration.ofSeconds(30))
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> {
                    given()
                            .when().get(jobUrl + "/phase")
                            .then()
                            .statusCode(200)
                            .body(not(comparesEqualTo("RUNNING")));
                });

        String status = given()
                .when().get(jobUrl + "/phase")
                .then()
                .statusCode(200)
                .extract().body().asString();

        if (status.equals("ERROR")) {
            given()
                    .when().get(jobUrl + "/error")
                    .then()
                    .statusCode(200)
                    .log().body();
            fail("Job ended in error state");
        }
        else if (status.equals("COMPLETED")) {
            // Retrieve Results
            given()
                    .when().get(jobUrl + "/results")
                    .then()
                    .log().ifValidationFails(LogDetail.BODY)
                    .statusCode(200)
                    .body("results.result.size()", greaterThan(0));

            //retrieve the actual result
            given()
                    .when().get(jobUrl + "/results/result")
                    .then()
                    .statusCode(200)
                    .log().body();

            //TODO get the result into a file and verify that it is an OK VOTable.

        }
        else
        {
            fail("Unexpected job status "+ status);
        }
    }
}

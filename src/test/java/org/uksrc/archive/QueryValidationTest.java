package org.uksrc.archive;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.Response;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static io.restassured.RestAssured.given;

import static jakarta.ws.rs.core.Response.Status.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Intended for the use of testing the TAP ADQL service with queries.
 */
@QuarkusTest
public class QueryValidationTest {
    @Inject
    EntityManager em;

    private static final String BOLD   = "\u001B[1m";
    private static final String RESET  = "\u001B[0m";

    //http://localhost:8080/archive/tap/sync?REQUEST=doQuery&LANG=ADQL&FORMAT=VOTABLE&QUERY=SELECT%20*%20FROM%20caom2.Observation%20WHERE%20DISTANCE(160.0%2C%200%2C%20180.0%2C%200.0)%20%3C%201.0

    private static final String TAP_QUERY = "/tap/sync?REQUEST=doQuery&LANG=ADQL&FORMAT=VOTABLE&QUERY=";

    @BeforeAll
    public static void init(){
        //VOLLT_BASE_PATH="C:\Development\caom\archive-service\src\main\resources\META-INF\resources"
       // checkEnvironment();
        Path projectRoot = Paths.get("").toAbsolutePath(); // current working dir
        Path resourceDir = projectRoot.resolve("src/main/resources/META-INF/resources");

        String absolutePath = resourceDir.toString();
        System.out.println("VOLLT_BASE_PATH = " + absolutePath);

        // Make it available to the launched service if it reads env vars
        System.setProperty("VOLLT_BASE_PATH", absolutePath);
    }

    //BeforeAll doesn't allow the HTTP call unfortunately
    @BeforeEach
    public void setup() throws IOException {
        Response res = given()
                .contentType("application/xml")
                .when()
                .get("/observations/2cf99e88-90e1-4fe8-a502-e5cafdc6ffa1") //MUST be the same as the caom2:Id value in the supplied file
                .andReturn();

        if (res.statusCode() == NOT_FOUND.getStatusCode()) {
            String xml = Files.readString(Paths.get("testing/observation1.xml"));

            given()
                    .contentType("application/xml")
                    .body(xml)
                    .when()
                    .post("/observations")
                    .then()
                    .statusCode(CREATED.getStatusCode());
        }
    }

    //DISABLED until the TAP service is changed, the current Vollt service isn't initialising in test.
    @Test
    public void testDistance() throws IOException {
        String request = TAP_QUERY + URLEncoder.encode("SELECT * FROM TAP_SCHEMA.tables", StandardCharsets.UTF_8);
        Response res = given()
               // .contentType("application/xml")
                .when()
                //.get(request)
                .get(TAP_QUERY + "SELECT * FROM TAP_SCHEMA.tables")
                .andReturn();

        String body = res.getBody().asString();
        System.out.println(body);

        assertEquals(res.statusCode(), OK.getStatusCode());
    }

    /**
     * Determines whether the required environment variable is set for running the Vollt TAP service servlet in a test profile.
     * Required ENV:VOLLT_BASE_PATH set to the /resources folder of the local deployment.
     */
    private static void checkEnvironment() {
        if (System.getenv("VOLLT_BASE_PATH") == null) {
            boolean ansi = System.console() != null && System.getenv().get("TERM") != null;
            if (ansi) {
                System.out.println(BOLD + "VOLLT_BASE_PATH is not defined!" + RESET);
            }
            else {
                System.out.println("VOLLT_BASE_PATH is not defined!");
            }
            System.out.println("Suggested value should be to the absolute path to /archive-service/src/main/resources/META-INF/resources");

            assumeTrue(System.getenv("VOLLT_BASE_PATH") != null);
        }
    }
}

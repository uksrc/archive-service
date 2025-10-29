package org.uksrc.archive;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.Response;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static io.restassured.RestAssured.given;

import static jakarta.ws.rs.core.Response.Status.*;

/**
 * Intended for the use of testing the TAP ADQL service with queries.
 */
@QuarkusTest
public class QueryValidationTest {
    @Inject
    EntityManager em;

    //http://localhost:8080/archive/tap/sync?REQUEST=doQuery&LANG=ADQL&FORMAT=VOTABLE&QUERY=SELECT%20*%20FROM%20caom2.Observation%20WHERE%20DISTANCE(160.0%2C%200%2C%20180.0%2C%200.0)%20%3C%201.0

    private static final String TAP_QUERY = "/tap/sync?REQUEST=doQuery&LANG=ADQL&FORMAT=VOTABLE&QUERY=";

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
//    @Test
//    public void testDistance() throws IOException {
//        Response res = given()
//                .contentType("application/xml")
//                .when()
//                .get(TAP_QUERY + "SELECT%20*%20FROM%20Observation%3B")
//                .andReturn();
//
//        assertEquals(res.statusCode(), OK.getStatusCode());
//    }
}

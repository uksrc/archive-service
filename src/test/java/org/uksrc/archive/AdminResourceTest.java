package org.uksrc.archive;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static jakarta.ws.rs.core.Response.Status.*;

/**
 * Unit tests to allow the testing of the /admin API, should contain anything that requires
 * basic authentication (username and password).
 */
@QuarkusTest
public class AdminResourceTest {

    @ConfigProperty(name = "basic.auth.admin.username")
    String adminUsername;

    @ConfigProperty(name = "basic.auth.admin.password")
    String adminPassword;

    private final static String OBSERVATION = "<caom2:Observation xmlns:caom2=\"http://www.opencadc.org/caom2/xml/v2.5\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"caom2:SimpleObservation\" caom2:id=\"" + UUID.randomUUID() + "\">\n" +
            "<caom2:collection>CK2235</caom2:collection>\n" +
            "<caom2:uri>https://www.archive-service.org/CK2/CK2235/CK2235_L_002_20180714</caom2:uri>\n" +
            "<caom2:uriBucket>3d0</caom2:uriBucket>\n" +
            "<caom2:intent>science</caom2:intent>\n" +
            "</caom2:Observation>";

    // Should successfully return the landing page for the /admin endpoint.
    @Test
    public void getDefaultAdminPageMessage() {
        given()
                .auth().preemptive().basic(adminUsername, adminPassword)
                .when()
                .get("/admin")
                .then()
                .statusCode(OK.getStatusCode());
    }

    // Should allow the injection of a new resource using basic authentication
    @Test
    public void addingAResource() {
        given()
                .auth().preemptive().basic(adminUsername, adminPassword)
                .contentType(ContentType.XML)
                .body(OBSERVATION)
                .when()
                .post("/admin/addObservation")
                .then()
                .statusCode(CREATED.getStatusCode());
    }

    // Tests the failure response for not supplying the basic authentication values (username & password)
    @Test
    public void addingAResourceWithoutAuth() {
        given()
                .contentType(ContentType.XML)
                .body(OBSERVATION)
                .when()
                .post("/admin/addObservation")
                .then()
                .statusCode(UNAUTHORIZED.getStatusCode());
    }
}

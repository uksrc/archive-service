package org.uksrc.archive;

import io.quarkus.test.junit.QuarkusTest;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

@QuarkusTest
public class AdminResourceTest {

    @ConfigProperty(name = "basic.auth.admin.username")
    String adminUsername;

    @ConfigProperty(name = "basic.auth.admin.password")
    String adminPassword;



    @Test
    public void getDefaultAdminPageMessage() {
        given()
                .auth().preemptive().basic(adminUsername, adminPassword)
                .when()
                .get("/admin")
                .then()
                .statusCode(200);
    }
}

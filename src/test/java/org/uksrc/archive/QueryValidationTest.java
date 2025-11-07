package org.uksrc.archive;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.response.Response;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import org.ivoa.dm.caom2.DerivedObservation;
import org.ivoa.dm.caom2.Observation;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Paths;

import static io.restassured.RestAssured.given;

import static jakarta.ws.rs.core.Response.Status.*;
import static org.hamcrest.Matchers.*;
import static org.uksrc.archive.utils.Utilities.TEST_READER_ROLE;
import static org.uksrc.archive.utils.Utilities.TEST_WRITER_ROLE;

/**
 * Intended for the use of testing the TAP ADQL service with queries.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@QuarkusTest
public class QueryValidationTest {

    @Inject
    EntityManager em;

    @Inject
    ObservationResource observationResource;

    private static boolean dataLoaded = false;

    //http://localhost:8080/archive/tap/sync?REQUEST=doQuery&LANG=ADQL&FORMAT=VOTABLE&QUERY=SELECT%20*%20FROM%20caom2.Observation%20WHERE%20DISTANCE(160.0%2C%200%2C%20180.0%2C%200.0)%20%3C%201.0

    private static final String TAP_QUERY = "/tap/sync?REQUEST=doQuery&LANG=ADQL&FORMAT=%s&QUERY=";

    /**
     * Unfortunately, BeforeAll & BeforeEach cannot be used to add an Observation due to security and API startup timing.
     * Needs to be called before ALL tests, so testing an individual test would require a called to SetupData() first.
     * @throws IOException Error reading the example Observation file.
     * @throws JAXBException Error parsing the example Observation XML.
     */
    @Test
    @Order(1)
    @TestSecurity(user = "testuser", roles = {TEST_READER_ROLE, TEST_WRITER_ROLE})
    void setupData() throws IOException, JAXBException {
        if (!dataLoaded) {
            String xml = Files.readString(Paths.get("testing/observation1.xml"));

            JAXBContext jaxbContext = JAXBContext.newInstance(DerivedObservation.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            Object result = unmarshaller.unmarshal(new StringReader(xml));

            Observation obs;
            if (result instanceof JAXBElement<?> jaxbElement) {
                obs = (Observation) jaxbElement.getValue();
            } else {
                obs = (Observation) result;
            }
            observationResource.addObservation(obs);

            dataLoaded = true;
        }
    }

    @Test
    @DisplayName("Select the standard TAP_SCHEMA tables")
    public void testSchemas() {
        String request = String.format(TAP_QUERY, "VOTABLE") + "SELECT * FROM TAP_SCHEMA.tables";
        Response res = queryRequest(request);
        res.then()
                .statusCode(OK.getStatusCode())
                .body("VOTABLE.RESOURCE.INFO.find {it.@name == 'QUERY'}.@value",
                        equalTo("SELECT * FROM TAP_SCHEMA.tables"));
    }

    @Test
    @DisplayName("Distance with individual values (No Points)")
    public void testDistance() {
        String request = String.format(TAP_QUERY, "JSON") + "SELECT * FROM caom2.\"point\" as po WHERE DISTANCE(po.cval1, po.cval2, 150.0, 2.5) < 90.0;";
        Response res = queryRequest(request);

        //String jsonPathExpression = "metadata.find { it.name == 'ID' }.datatype";

        res.then()
                .statusCode(OK.getStatusCode())
                .body("data.size()", equalTo(2));
                //.body(jsonPathExpression, equalTo("LONG"));
    }

    @Test
    @DisplayName("BOX with individual values and no geometric co-ord system defined")
    public void testBox() {
        String request = String.format(TAP_QUERY, "JSON") + "SELECT * FROM \"point\" as po WHERE 1 = CONTAINS(POINT(cval1, cval2), BOX(190.0, 56.0, 10.0, 10.0));";

        Response res = queryRequest(request);
        res.then()
                .statusCode(OK.getStatusCode())
                .body("data.size()", equalTo(1));
    }

    @Test
    @DisplayName("CIRCLE with individual values and no geometric co-ord system defined")
    public void testCircle() {
        String request = String.format(TAP_QUERY, "JSON") + "SELECT * FROM point as po WHERE CONTAINS(POINT(po.cval1, po.cval2), CIRCLE(195.0, 57.0, 5.0)) = 1;";

        Response res = queryRequest(request);
        res.then()
                .statusCode(OK.getStatusCode())
                .body("data", hasSize(2))
                .body("metadata.name", containsInAnyOrder("cval1", "cval2", "ID", "POLYGON_ID"));
    }

    @Test
    @DisplayName("POLYGON with individual values and no geometric co-ord system defined")
    public void testPolygon() {
        String request = String.format(TAP_QUERY, "JSON") + "SELECT * FROM \"point\" as po WHERE 1 = CONTAINS(POINT(po.cval1, po.cval2), POLYGON(180.0, 50.0, 200.0, 50.0, 200.0, 70.0, 180.0, 70.0));";

        Response res = queryRequest(request);
        res.then()
                .statusCode(OK.getStatusCode())
                .body("data", hasSize(2))
                .body("metadata.name", containsInAnyOrder("cval1", "cval2", "ID", "POLYGON_ID"));
    }

    @Test
    @DisplayName("POLYGON with individual values and no geometric co-ord system defined (no results)")
    public void testPolygonFailure() {
        String request = String.format(TAP_QUERY, "JSON") + "SELECT * FROM \"point\" as po WHERE 1 = CONTAINS(POINT(po.cval1, po.cval2), POLYGON(100.0, 50.0, 110.0, 50.0, 110.0, 70.0, 100.0, 70.0));";

        Response res = queryRequest(request);
        res.then()
                .statusCode(OK.getStatusCode())
                .body("data", hasSize(0))
                .body("metadata.name", containsInAnyOrder("cval1", "cval2", "ID", "POLYGON_ID"));
    }

    @Test
    @DisplayName("REGION with POINT")
    public void testPolygonWithPoint() {
        String request = String.format(TAP_QUERY, "JSON") + "SELECT * FROM \"point\" as po WHERE 1 = CONTAINS(POINT(po.cval1, po.cval2), REGION('CIRCLE ICRS 190.0 50.0 7.0'));";
        double expectedCval1 = 193.109524583333;        //Must match the value from the test file (observation1.xml)
        double tolerance = 0.00001;

        Response res = queryRequest(request);
        res.then()
                .statusCode(OK.getStatusCode())
                .body("data", hasSize(1))
                .body("metadata.name", containsInAnyOrder("cval1", "cval2", "ID", "POLYGON_ID"))
                .body("data[0][0].toDouble()", closeTo(expectedCval1, tolerance));  //Value from database should be a double regardless but "cast" still required.
    }

    @Test
    @DisplayName("REGION (with an incorrect geometric co-ord system defined) with POINT")
    public void testRegionWithIncorrectGeometricCoOrd() {
        String request = String.format(TAP_QUERY, "JSON") + "SELECT * FROM \"point\" WHERE CONTAINS(POINT(180.0, 0.0), REGION('CIRCLE MADE_UP 180.0 0.0 5.0')) = 1;";

        Response res = queryRequest(request);
        res.then()
                .statusCode(BAD_REQUEST.getStatusCode())
                .body(containsString("Unsupported region serialization"));
    }


    /**
     * Perform an AQDL query.
     * All queries are performed against the localhost instance of the archive-service.
     * @param query The ADQL query (NOT URL encoded)
     * @return The raw response from the target
     */
    private Response queryRequest(String query) {
        return given()
                .contentType("application/xml")
                .when()
                .get(query)
                .andReturn();
    }
}

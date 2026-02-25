package org.uksrc.archive;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.response.Response;
import jakarta.inject.Inject;
import org.ivoa.dm.caom2.Observation;
import org.ivoa.dm.caom2.TargetPosition;
import org.ivoa.dm.caom2.types.Point;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.*;
import org.uksrc.archive.utils.ObservationListWrapper;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static io.restassured.RestAssured.given;
import static jakarta.ws.rs.core.Response.Status.OK;
import static org.junit.jupiter.api.Assertions.*;
import static org.uksrc.archive.utils.Utilities.*;

/**
 * Tests the /search/cone endpoint.
 * Uses a single target position to test against, with a list of positions to search around.
 * Intended to be a fine-grained test of the cone search functionality, using the coneTestData.json file.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@QuarkusTest
public class SearchResourceTest {

    @Inject
    ObservationResource observationResource;

    private static JSONObject positions;
    private static boolean dataLoaded = false;

    //Load all the test data values that are either inside or outside the target cone (also contains the target position).
    @BeforeAll
    public static void loadData() {
        try {
            String positionData = Files.readString(Paths.get("testing/coneTestData.json"));
            positions = new JSONObject(positionData);
        } catch (IOException e) {
            fail("Unable to load position data from file: testing/coneTestData.json");
        }
    }

    @Test
    @Order(1)
    @TestSecurity(user = "testuser", roles = {TEST_READER_ROLE, TEST_WRITER_ROLE})
    void setupData() {
        if (!dataLoaded) {
            try {
                Observation obs = readXmlFile("testing/observationTargeted.xml", Observation.class);
                obs.setTargetPosition(createTargetPosition());
                observationResource.addObservation(obs);
                dataLoaded = true;
            }
            catch (Exception e) {
                fail("Failed to load test data", e);
            }
        }
    }

    /**
     * Tests the functionality of the Cone Search API endpoint.
     * This is for the dedicated cone search API
     */
    @Test
    @TestSecurity(user = "testuser", roles = {TEST_READER_ROLE, TEST_WRITER_ROLE})
    void testConeSearchAPI() {
        testConeSearch("/search/cone");
    }

    /**
     * Tests the functionality of the Search API endpoint.
     * This is for the filter search API, where parameters other than the cone search can be supplied.
     */
    @Test
    @TestSecurity(user = "testuser", roles = {TEST_READER_ROLE, TEST_WRITER_ROLE})
    void testSearchAPI() {
        testConeSearch("/search");
    }

    /**
     * Will perform a cone search as the specified API endpoint.
     * @param api The API to use to perform the cone search. Must support parameters ra, dec and radius.
     */
    private void testConeSearch(String api) {
        JSONObject target = positions.getJSONObject("target");
        double radius = target.getDouble("radius");

        JSONArray positionList = positions.getJSONArray("positions");
        for(int i = 0; i < positionList.length(); i++) {
            JSONObject position = positionList.getJSONObject(i);

            String query = api + "?ra=" + position.getDouble("ra") + "&dec=" + position.getDouble("dec") + "&radius=" + radius;
            int minExpected = position.getBoolean("withinRadius") ? 1 : 0;

            Response res = given()
                    .contentType("application/xml")
                    .when()
                    .get(query)
                    .andReturn();

            assertEquals (OK.getStatusCode(), res.getStatusCode());

            try {
                String xml = res.getBody().asString();
                ObservationListWrapper wrapper = readXmlString(xml, ObservationListWrapper.class);

                List<Observation> observations = wrapper.getObservations();
                assertTrue(observations.size() >= minExpected,
                        "Expected at least " + minExpected + " observations");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Uses the values for 'target' from positions to make a TargetPosition object.
     * Will be used to test against.
     * @see "testing/coneTestData.json"
     * @return TargetPosition containing the values from the 'target' object in positions.
     */
    private TargetPosition createTargetPosition(){
        JSONObject position = positions.getJSONObject("target");

        TargetPosition tp = new TargetPosition();
        tp.setCoordsys("ICRS");
        Point point = new Point();
        point.setCval1(position.getDouble("ra"));
        point.setCval2(position.getDouble("dec"));
        tp.setCoordinates(point);

        return tp;
    }
}

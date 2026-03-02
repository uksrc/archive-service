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

    /**
     * Loads data into the database for testing.
     * Done this way to avoid issues with timing on the database startup (as opposed to in @BeforeAll).
     */
    @Test
    @Order(1)
    @TestSecurity(user = "testuser", roles = {TEST_READER_ROLE, TEST_WRITER_ROLE})
    void setupData() {
        if (!dataLoaded) {
            try {
                //Test for cone positions
                Observation obs = readXmlFile("testing/observationTargeted.xml", Observation.class);
                obs.setTargetPosition(createTargetPosition());
                observationResource.addObservation(obs);

                //Test resource for filtered parameters
                obs = readXmlFile("testing/observationFiltered.xml", Observation.class);
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
     * Tests the functionality of the Search API for a specific target object.
     * This method verifies that the search endpoint correctly retrieves observations
     * related to the specified astronomical target.
     * <p>
     * Preconditions:
     * - observationFiltered.xml is loaded into the database.
     * <p>
     * Test Details:
     * - Sends a search query targeting the object "M31".
     * - Asserts that the number of retrieved observations matches the expected value.
     */
    @Test
    @TestSecurity(user = "testuser", roles = {TEST_READER_ROLE, TEST_WRITER_ROLE})
    void testSearchTarget() {
        String query = "/search?target=M31";

        searchIncidence(query, 1);
    }

    /**
     * Tests the functionality of the Search API for a specific target object.
     * This method verifies that the search endpoint correctly retrieves observations
     * related to the specified astronomical target.
     * <p>
     * Preconditions:
     * - observationFiltered.xml is loaded into the database.
     * <p>
     * Test Details:
     * - Sends a search query targeting the object "NotPresent" (which should not exist).
     * - Asserts that the number of retrieved observations matches the expected value.
     */
    @Test
    @TestSecurity(user = "testuser", roles = {TEST_READER_ROLE, TEST_WRITER_ROLE})
    void testInvalidSearchTarget() {
        String query = "/search?target=NotPresent";

        searchIncidence(query, 0);
    }

    /**
     * Tests the functionality of the Search API for filtering based on the observation band.
     * This method verifies that the search endpoint correctly retrieves observations
     * related to specified wavelength bands such as "radio" and "UV".
     * <p>
     * Preconditions:
     * - The appropriate test data must be preloaded into the database to ensure
     *   observations are available for the specified bands.
     * <p>
     * Assumptions:
     * - Multiple bands can coexist within the same observation.
     * <p>
     * Assertions:
     * - Ensures that the observations filtered by the "radio" band result in the expected count.
     * - Ensures that the observations filtered by the "UV" band result in the expected count.
     */
    @Test
    @TestSecurity(user = "testuser", roles = {TEST_READER_ROLE, TEST_WRITER_ROLE})
    void testSearchBand() {
        //More than one band can exist in the same observation.
        String query = "/search?band=radio";
        searchIncidence(query, 1);

        query = "/search?band=UV";
        searchIncidence(query, 1);
    }

    /**
     * Validates that the Search API correctly handles cases where no observations
     * match the specified wavelength band filter. This test ensures the system
     * can handle searches for bands that have no associated observations.
     * <p>
     * Preconditions:
     * - Test data must be preloaded into the database, ensuring that no observations
     *   exist for the specified bands ("Infrared" and "Xray").
     * <p>
     * Assertions:
     * - Ensures that the number of observations returned for each query
     *   matches the expected value (0 in this case).
     */
    @Test
    @TestSecurity(user = "testuser", roles = {TEST_READER_ROLE, TEST_WRITER_ROLE})
    void testMissingSearchBand() {
        //More than one band can exist in the same observation.
        String query = "/search?band=Infrared";
        searchIncidence(query, 0);

        query = "/search?band=Xray";
        searchIncidence(query, 0);
    }

    /**
     * Searches for observations based on the given query and verifies the number of results.
     *
     * @param query The API endpoint query string used to search for observations.
     *              This should define the search parameters.
     * @param numExpected The expected number of observations to be returned
     *                    by the query. Used for assertion.
     */
    private void searchIncidence(String query, int numExpected){
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
            assertEquals(numExpected, observations.size(),
                    "Expected " + numExpected + " observations");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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

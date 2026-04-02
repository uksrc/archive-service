package org.uksrc.archive;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.response.Response;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
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
import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
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
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class SearchResourceTest {

    @Inject
    ObservationResource observationResource;

    @Inject
    EntityManager em;

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
    @TestSecurity(user = TEST_USER, roles = {TEST_READER_ROLE, TEST_WRITER_ROLE})
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

    @AfterAll
    @Transactional
    public void clearDatabase() {
        // Clear the table(s)
        em.createQuery("DELETE FROM Artifact").executeUpdate();
        em.createQuery("DELETE FROM Plane").executeUpdate();
        em.createQuery("DELETE FROM Observation").executeUpdate();
    }

    /**
     * Tests the functionality of the Cone Search API endpoint.
     * This is for the dedicated cone search API
     */
    @Test
    @TestSecurity(user = TEST_USER, roles = {TEST_READER_ROLE})
    void testConeSearchAPI() {
        testConeSearch("/search/cone");
    }

    /**
     * Tests the functionality of the Search API endpoint.
     * This is for the filter search API, where parameters other than the cone search can be supplied.
     */
    @Test
    @TestSecurity(user = TEST_USER, roles = {TEST_READER_ROLE, TEST_WRITER_ROLE})
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
    @TestSecurity(user = TEST_USER, roles = {TEST_READER_ROLE})
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
    @TestSecurity(user = TEST_USER, roles = {TEST_READER_ROLE})
    void testInvalidSearchTarget() {
        String query = "/search?target=NotPresent";

        searchIncidence(query, 0);
    }

    /**
     * Tests the functionality of the Search API for filtering based on the project identifier.
     * This method verifies that the search endpoint correctly retrieves observations
     * associated with a specific project.
     * <p>
     * Preconditions:
     * - The database must be preloaded with data containing observations associated with "projectNum1".
     * <p>
     * Assertions:
     * - Verifies that the result of the search matches the expected number of observations.
     */
    @Test
    @TestSecurity(user = TEST_USER, roles = {TEST_READER_ROLE})
    void testSearchProject() {
        String query = "/search?project=projectNum1";

        searchIncidence(query, 1);
    }

    /**
     * Tests the behaviour of the Search API when queried with an invalid project identifier.
     * This method verifies that the search endpoint returns zero observations when the specified
     * project does not exist in the database.
     *
     * Preconditions:
     * - The database must be preloaded with test data that does not include observations for
     *   the specified project ("NotPresent").
     */
    @Test
    @TestSecurity(user = TEST_USER, roles = {TEST_READER_ROLE})
    void testInvalidSearchProject() {
        String query = "/search?project=NotPresent";

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
    @TestSecurity(user = TEST_USER, roles = {TEST_READER_ROLE})
    void testSearchBand() {
        //More than one band can exist in the same observation.
        String query = "/search?band=C";
        searchIncidence(query, 1);

        query = "/search?band=L";
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
    @TestSecurity(user = TEST_USER, roles = {TEST_READER_ROLE})
    void testMissingSearchBand() {
        //More than one band can exist in the same observation.
        String query = "/search?band=Infrared";
        searchIncidence(query, 0);

        query = "/search?band=Xray";
        searchIncidence(query, 0);
    }

    /**
     * Tests the Search API functionality for filtering observations based on the start date.
     * This method verifies that the search endpoint correctly retrieves observations
     * when the query parameter `startDate` is provided, using both date-only and date-time formats.
     * <p>
     * Preconditions:
     * - Test data containing observations corresponding to the specified dates must be preloaded
     *   into the database.
     * <p>
     * Assertions:
     * - Should return any resources that have a recorded date beyond the date specified in the query.
     */
    @Test
    @TestSecurity(user = TEST_USER, roles = {TEST_READER_ROLE})
    void testSearchStartDate() {
        //Date and time
        String query = "/search?startDate=2019-08-02T00:02:06Z";
        searchIncidence(query, 1);

        //Date only
        query = "/search?startDate=2019-08-02";
        searchIncidence(query, 1);
    }

    /**
     * Tests the functionality of the Search API for filtering by an expired start date.
     * This method verifies that the search endpoint correctly handles queries where the start date
     * is set to a future date beyond the range of the data stored in the database.
     * <p>
     * Preconditions:
     * - The database contains observation data with dates earlier than the specified query date.
     * <p>
     * Assertions:
     * - Should return no resources when the query date is set to a future date.
     */
    @Test
    @TestSecurity(user = TEST_USER, roles = {TEST_READER_ROLE})
    void testSearchExpiredDate(){
        String query = "/search?startDate=2031-08-02T00:02:06Z";
        searchIncidence(query, 0);
    }

    /**
     * Tests the handling of invalid start date format in the Search API.
     * <p>
     * Preconditions:
     * - No specific data loading is required for this test, as it verifies input validation at the API level.
     * <p>
     * Assertions:
     * - Verifies that the response status code matches the HTTP `400 BAD REQUEST` status, indicating that the
     *   server correctly validates input and rejects the malformed date format.
     */
    @Test
    @TestSecurity(user = TEST_USER, roles = {TEST_READER_ROLE})
    void testSearchStartDateInvalidFormat() {
        String query = "/search?startDate=2001-MAY-13th";
        Response res = given()
                .contentType("application/xml")
                .when()
                .get(query)
                .andReturn();

        assertEquals (BAD_REQUEST.getStatusCode(), res.getStatusCode());
    }

    /**
     * Tests the search functionality for a given frequency range.
     * This method verifies that the search API correctly processes and returns the expected results
     * when queried with a specific minimum and maximum frequency range.
     * NOTE: As CAOM2.5 values are stored as wavelength(m), this should also test that the conversion
     * is handled correctly.
     * <p>
     * Preconditions:
     * - The database contains observation data with frequency ranges outside the search criteria.
     * <p>
     * Assertions:
     * - Should return a resource that matches the specified frequency range.
     */
    @Test
    @TestSecurity(user = TEST_USER, roles = {TEST_READER_ROLE})
    void testSearchFrequency() {
        String query = "/search?freqMin=30&freqMax=40"; //Hz
        searchIncidence(query, 1);
    }

    /**
     * Tests the behaviour of the search functionality when provided with a frequency range that is outside
     * the range of the existing resources.
     * <p>
     * Preconditions:
     * - The database contains observation data with dates earlier than the specified query date.
     * <p>
     * Postconditions:
     * - The search functionality should respond without errors and return the expected result,
     *   which in this case is no records since the frequency range is outside the range of the data.
     */
    @Test
    @TestSecurity(user = TEST_USER, roles = {TEST_READER_ROLE})
    void testSearchInvalidFrequency() {
        String query = "/search?freqMin=130&freqMax=140"; //Hz
        searchIncidence(query, 0);
    }

    /**
     * Tests the behaviour of the telescope name query parameter in the search endpoint.
     * <p>
     * This test validates that searching for a specific telescope, in this case "e-MERLIN",
     * returns the expected result. The method asserts that the search operation successfully
     * identifies and matches the provided telescope parameter.
     * <p>
     * Preconditions:
     * - The database contains observation data with a telescope named "e-MERLIN".
     * <p>
     * Postconditions:
     * - The search functionality should respond without errors and return the expected observation.
     */
    @Test
    @TestSecurity(user = TEST_USER, roles = {TEST_READER_ROLE})
    void testTelescopeName() {
        String query = "/search?telescope=e-MERLIN";
        searchIncidence(query, 1);
    }

    /**
     * Tests the behaviour of the telescope name query parameter in the search endpoint.
     * <p>
     * This test validates that searching for a specific telescope, in this case "VLBI",
     * returns nothing.
     * <p>
     * Preconditions:
     * - The database contains observation data without a telescope named "VLBI".
     * <p>
     * Postconditions:
     * - The search functionality should respond without errors and return no observations.
     */
    @Test
    @TestSecurity(user = TEST_USER, roles = {TEST_READER_ROLE})
    void testIncorrectTelescopeName() {
        String query = "/search?telescope=VLBI";
        searchIncidence(query, 0);
    }

    /**
     * Tests the behaviour of the instrument name query parameter in the search endpoint.
     * <p>
     * This test validates that searching for a specific instrument, in this case "array123",
     * returns the expected result. The method asserts that the search operation successfully
     * identifies and matches the provided instrument parameter.
     * <p>
     * Preconditions:
     * - The database contains observation data with an instrument named "array123".
     * <p>
     * Postconditions:
     * - The search functionality should respond without errors and return the expected observation.
     */
    @Test
    @TestSecurity(user = TEST_USER, roles = {TEST_READER_ROLE})
    void testInstrumentName() {
        String query = "/search?instrument=array123";
        searchIncidence(query, 1);
    }

    /**
     * Tests the behaviour of the instrument name query parameter in the search endpoint.
     * <p>
     * This test validates that searching for a specific instrument, in this case "not-found",
     * returns nothing.
     * <p>
     * Preconditions:
     * - The database contains observation data without an instrument named "not-found".
     * <p>
     * Postconditions:
     * - The search functionality should respond without errors and return no observations.
     */
    @Test
    @TestSecurity(user = TEST_USER, roles = {TEST_READER_ROLE})
    void testIncorrectInstrumentName() {
        String query = "/search?instrument=not-found";
        searchIncidence(query, 0);
    }

    /**
     * Tests the functionality of the Search API for a specific collection.
     * This method verifies that the search endpoint correctly retrieves observations
     * related to the specified collection, such as "EMERLIN" or "VLBI".
     * <p>
     * Preconditions:
     * - observationFiltered.xml is loaded into the database.
     * - observationTargeted.xml is loaded into the database.
     * <p>
     * Test Details:
     * - Sends a search query for the collection "EMERLIN".
     * - Asserts that the number of retrieved observations matches the expected value.
     */
    @Test
    @TestSecurity(user = TEST_USER, roles = {TEST_READER_ROLE})
    void testSearchCollection() {
        String query = "/search?collection=EMERLIN";

        searchIncidence(query, 2);
    }

    /**
     * Tests the functionality of the Search API for a specific collection and target to check that more than one parameter can
     * be filtered at once.
     * This method verifies that the search endpoint correctly retrieves observations
     * related to the specified collection, such as "EMERLIN" or "VLBI" and target.
     * <p>
     * Preconditions:
     * - observationFiltered.xml is loaded into the database.
     * - observationTargeted.xml is loaded into the database.
     * <p>
     * Test Details:
     * - Sends a search query for the collection "EMERLIN".
     * - Sends a search query targeting the object "M31".
     * - Asserts that the number of retrieved observations matches the expected value.
     */
    @Test
    @TestSecurity(user = TEST_USER, roles = {TEST_READER_ROLE})
    void testSearchCollectionAndTarget() {
        String query = "/search?collection=EMERLIN&target=M31";

        searchIncidence(query, 1);
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

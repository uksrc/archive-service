package org.uksrc.archive;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.response.Response;
import jakarta.inject.Inject;
import org.ivoa.dm.caom2.Observation;
import org.ivoa.dm.caom2.TargetPosition;
import org.ivoa.dm.caom2.types.Point;
import org.jastronomy.jsofa.JSOFA;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.uksrc.archive.utils.ObservationListWrapper;

import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static jakarta.ws.rs.core.Response.Status.OK;
import static org.junit.jupiter.api.Assertions.*;
import static org.uksrc.archive.utils.Utilities.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@QuarkusTest
public class SearchMultipleResourceTest {

    @Inject
    ObservationResource observationResource;

    private boolean dataLoaded = false;
    private final double RA = 123.456;
    private final double DEC = -78.901;
    private final double STEP_DEGREES = 0.001;
    private final int NUM_OBSERVATIONS = 10;

    @Test
    @Order(1)
    @TestSecurity(user = "testuser", roles = {TEST_READER_ROLE, TEST_WRITER_ROLE})
    void setupData() {
        if (!dataLoaded) {
            try {
                //Add n Observation records with a tiny difference in RA
                for (int i = 0; i < NUM_OBSERVATIONS; i++) {
                    Observation obs = readXmlFile("testing/observationTargeted.xml", Observation.class);
                    obs.setId(UUID.randomUUID().toString());
                    double raValue = RA + (STEP_DEGREES * i);
                    obs.setTargetPosition(createTargetPosition(raValue, DEC));
                    observationResource.addObservation(obs);
                }
                dataLoaded = true;
            }
            catch (Exception e) {
                fail("Failed to load test data", e);
            }
        }
    }

    @Test
    @TestSecurity(user = "testuser", roles = {TEST_READER_ROLE})
    void testConeSearch() {
        //Attempts to retrieve the 3 closest to RA & DEC from a total defined by NUM_OBSERVATIONS
        final int NUM_EXPECTED = 3;

        double testRadius = radiusForNIncludedPointsJSOFA(RA, DEC, STEP_DEGREES, NUM_EXPECTED);
        String query = "/search/cone?ra=" + RA + "&dec=" + DEC + "&radius=" + testRadius;

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
            assertTrue(observations.size() >= NUM_EXPECTED,
                    "Expected " + NUM_EXPECTED + " observations");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        System.out.println(res.getBody().asString());
    }

    /**
     * Creates a TargetPosition object from the supplied RA and DEC values.
     * Will be used to test against.
     * @see TargetPosition
     * @return TargetPosition containing the values supplied.
     */
    @SuppressWarnings("SameParameterValue")
    private TargetPosition createTargetPosition(double ra, double dec) {
        TargetPosition tp = new TargetPosition();
        tp.setCoordsys("ICRS");
        Point point = new Point();
        point.setCval1(ra);
        point.setCval2(dec);
        tp.setCoordinates(point);

        return tp;
    }

    /**
     * Returns the degrees away from the supplied RA, based on the number of steps.
     * @param raDeg The original RA position
     * @param decDeg The original DEC position
     * @param stepDeg The degrees between each point
     * @param n The index of the last point to include
     * @return The angular separation in degrees between the supplied RA and the n number of stepDeg
     */
    @SuppressWarnings("SameParameterValue")
    private double radiusForNIncludedPointsJSOFA(double raDeg, double decDeg, double stepDeg, int n) {
        if (n < 1) {
            throw new IllegalArgumentException("n must be >= 1");
        }

        double raRadians = Math.toRadians(raDeg);
        double decRadians = Math.toRadians(decDeg);

        // Compute RA for the last included point (index n-1)
        double raN = Math.toRadians(raDeg + stepDeg * (n - 1));
        double decN = Math.toRadians(decDeg);

        // angular separation in degrees
        return Math.toDegrees(JSOFA.jauSeps(raRadians, decRadians, raN, decN));
    }

}

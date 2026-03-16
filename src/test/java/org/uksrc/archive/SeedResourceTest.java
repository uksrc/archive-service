package org.uksrc.archive;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.ws.rs.core.Response;
import org.ivoa.dm.caom2.Observation;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.uksrc.archive.utils.ObservationListWrapper;
import org.uksrc.archive.utils.SeedEnabledProfile;
import org.uksrc.archive.utils.tools.Tools;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Integration test class to validate seeding of test data and correct behaviour of
 * queries in a Quarkus test environment.
 * <p>
 * The seeding is explicitly enabled by utilising the {@code SeedEnabledProfile},
 * ensuring that the test data required for the query is populated in the database.
 */
@QuarkusTest
@TestProfile(SeedEnabledProfile.class)  //Force the seeding of example resources.
public class SeedResourceTest {

    @Inject
    EntityManager em;

    private static int numberOfSeededResources = 0;

    /**
     * Validates the presence of seeded resources required for the test setup.
     * <p>
     * @throws RuntimeException if an error occurs while loading seed file names.
     */
    @BeforeAll
    public static void checkSeededResources() {
        try {
            List<String> files = Tools.loadSeedFileNames();
            if (files != null){
                numberOfSeededResources = files.size();
            }

        } catch (Exception e) {
            System.out.println("Error determining the number of seeded resources: " + e.getMessage());
        }
    }

    /**
     * Tests the behaviour of the query execution and response handling for retrieving
     * seeded observations (that are intended as examples).
     * <p>
     * The test verifies the following:
     * 1.The number of observations in the response matches the expected page size of 5.
     * <p>
     * The query retrieves Observation entities from the database, leveraging the
     * functionality provided by the {@code Tools.performQuery} method, which handles
     * pagination and wraps the results in a {@code Response}.
     */
    @Test
    public void test() {
        TypedQuery<Observation> query = em.createQuery("SELECT o FROM Observation o", Observation.class);

        Response res = Tools.performQuery(0, numberOfSeededResources, query);
        assertEquals(Response.Status.OK.getStatusCode(), res.getStatus());

        ObservationListWrapper wrapper = (ObservationListWrapper) res.getEntity();
        assertEquals(numberOfSeededResources, wrapper.getObservations().size());
    }
}

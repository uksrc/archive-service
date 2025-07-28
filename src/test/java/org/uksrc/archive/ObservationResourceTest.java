package org.uksrc.archive;

import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response;
import jakarta.xml.bind.JAXBElement;
import org.ivoa.dm.caom2.Observation;
import org.ivoa.dm.caom2.ObservationIntentType;
import org.ivoa.dm.caom2.SimpleObservation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.uksrc.archive.utils.ObservationListWrapper;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.uksrc.archive.utils.Utilities.*;

/**
 * Test class for the Observation class
 * Requirements:
 *  Postgres DB with the CAOM (2.5) models added (as tables). Should be in place automatically via the Quarkus mechanisms.
 */
@QuarkusTest
public class ObservationResourceTest {

    @Inject
    EntityManager em;

    @Inject
    ObservationResource observationResource;

    @BeforeEach
    @Transactional
    public void clearDatabase() {
        // Clear the table
        em.createQuery("DELETE FROM Observation").executeUpdate();
    }

    @Test
    @DisplayName("Check that and empty database returns a robust response.")
    @TestSecurity(user = "testuser", roles = {TEST_READER_ROLE})
    public void testGettingObservations() {
        try (Response res = observationResource.getAllObservations(null, null, null)) {
            assertEquals(Response.Status.OK.getStatusCode(), res.getStatus());

            ObservationListWrapper wrapper = (ObservationListWrapper) res.getEntity();
            assert(wrapper.getObservations().isEmpty());
        }
    }

    @Test
    @DisplayName("Add two observation and check two are returned.")
    @TestSecurity(user = "testuser", roles = {TEST_READER_ROLE, TEST_WRITER_ROLE})
    public void testGettingObservationsNonEmpty() {
        Observation obs1 = createSimpleObservation(OBSERVATION1, COLLECTION1);
        Observation obs2 = createSimpleObservation(OBSERVATION2, COLLECTION1);

        try(Response res1 = observationResource.addObservation(obs1);
            Response res2 = observationResource.addObservation(obs2)) {
            assertEquals (Response.Status.CREATED.getStatusCode(), res1.getStatus());
            assertEquals (Response.Status.CREATED.getStatusCode(), res2.getStatus());

            Response obsRes = observationResource.getAllObservations(null, null, null);
            assertEquals (Response.Status.OK.getStatusCode(), obsRes.getStatus());

            ObservationListWrapper wrapper = (ObservationListWrapper) obsRes.getEntity();
            List<Observation> observations = wrapper.getObservations();
            assertEquals(2, observations.size());
        }
    }

    @Test
    @DisplayName("Get observations via collection Id")
    @TestSecurity(user = "testuser", roles = {TEST_READER_ROLE, TEST_WRITER_ROLE})
    public void testGettingObservationsViaCollectionId() {
        Observation obs1 = createSimpleObservation(OBSERVATION1, COLLECTION1);
        Observation obs2 = createSimpleObservation(OBSERVATION2, COLLECTION1);

        try(Response res1 = observationResource.addObservation(obs1);
            Response res2 = observationResource.addObservation(obs2)) {
            assertEquals (Response.Status.CREATED.getStatusCode(), res1.getStatus());
            assertEquals (Response.Status.CREATED.getStatusCode(), res2.getStatus());

            //Both previously added observations should be returned
            Response obsRes = observationResource.getAllObservations(COLLECTION1, null, null);
            assertEquals (obsRes.getStatus(), Response.Status.OK.getStatusCode());

            ObservationListWrapper wrapper = (ObservationListWrapper) obsRes.getEntity();
            assertEquals(2, wrapper.getObservations().size());

            //Neither of the previously added observations should be returned (ONLY collection1 exists)
            obsRes = observationResource.getAllObservations(COLLECTION2, null, null);
            assertEquals (obsRes.getStatus(), Response.Status.OK.getStatusCode());

            wrapper = (ObservationListWrapper) obsRes.getEntity();
            assert (wrapper.getObservations().isEmpty());
        }
    }

    @ParameterizedTest
    @DisplayName("Add an observation and check that part of the response body matches.")
    @TestSecurity(user = "testuser", roles = {TEST_WRITER_ROLE})
    @ValueSource(strings = {"simple", "derived"})
    public void testAddingObservation(String observation) {
        //As the method enters twice, we need to enforce different observation IDs.
        Observation obs = switch (observation.toLowerCase()) {
            case "simple" -> createSimpleObservation(OBSERVATION1, COLLECTION1);
            case "derived" -> createDerivedObservation(OBSERVATION2, COLLECTION1);
            default -> throw new IllegalArgumentException("Unsupported observation type: " + observation);
        };

        try (Response res = observationResource.addObservation(obs)) {
            assertEquals(res.getStatus(), Response.Status.CREATED.getStatusCode());

            //Check the responce to see if the caller receives the added observation.
            JAXBElement<?> jaxbElement = (JAXBElement<?>) res.getEntity();
            Observation responseObs = (Observation) jaxbElement.getValue();

            assert(responseObs.getId().equals(obs.getId()));
            assert(responseObs.getIntent() == ObservationIntentType.SCIENCE);
        }
    }

    @Test
    @DisplayName("Attempt to add an Observation with a MUST property missing.")
    @TestSecurity(user = "testuser", roles = {TEST_WRITER_ROLE})
    public void testAddingIncompleteObservation() {
        SimpleObservation obs = new SimpleObservation();
        obs.setId(UUID.randomUUID().toString());
        obs.setUri("a_uri");
        obs.setCollection(COLLECTION1);

        try (Response res = observationResource.addObservation(obs)) {
            assertEquals(res.getStatus(), Response.Status.BAD_REQUEST.getStatusCode());
        }
    }

    @Test
    @DisplayName("Add an observation, update one of its values and update, check it's been updated correctly.")
    @TestSecurity(user = "testuser", roles = {TEST_WRITER_ROLE})
    public void testUpdatingObservation() {
        Observation observation = createSimpleObservation(OBSERVATION2, COLLECTION1);

        try (Response res = observationResource.addObservation(observation)) {
            assertEquals(res.getStatus(), Response.Status.CREATED.getStatusCode());
        }

        // Update it with a different value
        observation.setIntent(ObservationIntentType.CALIBRATION);

        try (Response res = observationResource.updateObservation(OBSERVATION2, observation)) {
            assertEquals(res.getStatus(), Response.Status.OK.getStatusCode());

            JAXBElement<?> jaxbElement = (JAXBElement<?>) res.getEntity();
            Observation updatedObservation = (Observation) jaxbElement.getValue();

            assertEquals(OBSERVATION2, updatedObservation.getId());
            assertEquals(ObservationIntentType.CALIBRATION, updatedObservation.getIntent());
        }

        // For completeness, we need to check that the actual entry is updated
        try (Response res = observationResource.getObservation(OBSERVATION2)) {
            assertEquals(res.getStatus(), Response.Status.OK.getStatusCode());

            JAXBElement<?> jaxbElement = (JAXBElement<?>) res.getEntity();
            Observation updatedObservation = (Observation) jaxbElement.getValue();

            assertEquals(OBSERVATION2, updatedObservation.getId());
            assertEquals(ObservationIntentType.CALIBRATION, updatedObservation.getIntent());
        }
    }

    @Test
    @DisplayName("Attempt to update a non-existent observation and check the not found status.")
    @TestSecurity(user = "testuser", roles = {TEST_WRITER_ROLE})
    public void testUpdatingNonExistingObservation() {
        Observation observation = createSimpleObservation(OBSERVATION2, COLLECTION1);
        observation.setIntent(ObservationIntentType.CALIBRATION);

        try (Response res = observationResource.updateObservation(OBSERVATION2, observation)) {
            assertEquals(res.getStatus(), Response.Status.NOT_FOUND.getStatusCode());
        }
    }

    @Test
    @DisplayName("Attempt to delete an observation.")
    @TestSecurity(user = "testuser", roles = {TEST_WRITER_ROLE})
    public void testDeletingObservation() {
        Observation observation = createSimpleObservation(OBSERVATION1, COLLECTION1);

        try (Response res = observationResource.addObservation(observation)) {
            assertEquals(res.getStatus(), Response.Status.CREATED.getStatusCode());
        }

        // Check it exists
        try (Response obsRes = observationResource.getObservation(OBSERVATION1)) {
            JAXBElement<?> jaxbElement = (JAXBElement<?>) obsRes.getEntity();

            Observation updatedObservation = (Observation) jaxbElement.getValue();
            assertEquals(obsRes.getStatus(), Response.Status.OK.getStatusCode());
            assertEquals(OBSERVATION1, updatedObservation.getId());
        }

        // Delete it
        try (Response res = observationResource.deleteObservation(OBSERVATION1)) {
            assertEquals(res.getStatus(), Response.Status.NO_CONTENT.getStatusCode());
        }
    }

    @Test
    @DisplayName("Test paging results, first page")
    @TestSecurity(user = "testuser", roles = {TEST_WRITER_ROLE})
    public void testPagingResults() {
        for (int i = 0; i < 15; i++){
            Observation observation = createSimpleObservation(OBSERVATION1 + i, COLLECTION1);
            try (Response res = observationResource.addObservation(observation)) {
                assert (res.getStatus() == Response.Status.CREATED.getStatusCode());
            }
        }

        try (Response res = observationResource.getAllObservations(null, 0, 10)) {
            assertEquals(res.getStatus(), Response.Status.OK.getStatusCode());

            ObservationListWrapper wrapper = (ObservationListWrapper) res.getEntity();
            assertEquals(10, wrapper.getObservations().size());
        }
    }


    @Test
    @DisplayName("Test paging results, second page")
    @TestSecurity(user = "testuser", roles = {TEST_WRITER_ROLE})
    public void testPagingResults2() {
        for (int i = 0; i < 15; i++){
            Observation observation = createSimpleObservation(OBSERVATION1 + i, COLLECTION1);
            try (Response res = observationResource.addObservation(observation)) {
                assert (res.getStatus() == Response.Status.CREATED.getStatusCode());
            }
        }

        try (Response res = observationResource.getAllObservations(null, 1, 10)) {
            assertEquals(res.getStatus(), Response.Status.OK.getStatusCode());

            ObservationListWrapper wrapper = (ObservationListWrapper) res.getEntity();
            assertEquals(5, wrapper.getObservations().size());
        }
    }

    @Test
    @DisplayName("Attempt to delete an observation that doesn't exist.")
    @TestSecurity(user = "testuser", roles = {TEST_WRITER_ROLE})
    public void testDeletingNonExistingObservation() {
        try (Response res = observationResource.deleteObservation(OBSERVATION1)) {
            assertEquals(res.getStatus(), Response.Status.NOT_FOUND.getStatusCode());
        }
    }

    @Test
    @DisplayName("Attempt to access a protected resource without rights")
    public void testAccessResourceWithoutRights() {
        Observation observation = createSimpleObservation(OBSERVATION1, COLLECTION1);

        // Expected response
        assertThrows(AuthenticationFailedException.class, () -> {
            observationResource.addObservation(observation);
        });
    }
}

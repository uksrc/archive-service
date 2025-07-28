package org.uksrc.archive;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response;
import org.ivoa.dm.caom2.Observation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.uksrc.archive.utils.Utilities;

import java.util.Arrays;
import java.util.List;

import static io.restassured.RestAssured.when;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.uksrc.archive.utils.Utilities.*;

@QuarkusTest
public class CollectionResourceTest {

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
    @DisplayName("Test retrieving collection Ids from empty DB")
    @TestSecurity(user = "testuser", roles = {TEST_READER_ROLE})
    public void testRetrieveCollectionIdsFromEmptyDB() {
        String collections = when()
                .get("/collections/")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .extract()
                .asString();

        assert(collections.isEmpty());
    }

    @Test
    @DisplayName("Test retrieving collection Ids")
    @TestSecurity(user = "testuser", roles = {TEST_READER_ROLE, TEST_WRITER_ROLE})
    public void testRetrievingCollectionIds() {
        try {
            for (int i = 1; i < 6; i++) {
                Observation obs = createSimpleObservation(Utilities.OBSERVATION1 + i, Utilities.COLLECTION1);

                try (Response res = observationResource.addObservation(obs)) {
                    assertEquals(res.getStatus(), Response.Status.CREATED.getStatusCode());
                } catch (Exception e) {
                    fail("Exception during addObservation: " + e.getMessage());
                }
            }

            for (int i = 6; i < 13; i++) {
                Observation obs = createSimpleObservation(Utilities.OBSERVATION2 + i, Utilities.COLLECTION2);

                try (Response res = observationResource.addObservation(obs)) {
                    assertEquals(res.getStatus(), Response.Status.CREATED.getStatusCode());
                } catch (Exception e) {
                    fail("Exception during addObservation: " + e.getMessage());
                }
            }

            String collections = when()
                    .get("/collections/")
                    .then()
                    .statusCode(Response.Status.OK.getStatusCode())
                    .extract()
                    .asString();

            //Split the response on the tab separator
            String[] collectionIds = collections.split("\t");
            List<String> names = Arrays.asList(collectionIds);

            assert (names.size() == 2);
            assert (names.contains(Utilities.COLLECTION1));
            assert (names.contains(Utilities.COLLECTION2));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

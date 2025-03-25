package org.uksrc.archive;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uksrc.archive.utils.Utilities;

import java.util.Arrays;
import java.util.List;

import static io.restassured.RestAssured.when;
import static org.uksrc.archive.utils.Utilities.TEST_ROLE;

@QuarkusTest
public class CollectionResourceTest {

    @Inject
    EntityManager em;

    @BeforeEach
    @Transactional
    public void clearDatabase() {
        // Clear the table
        em.createQuery("DELETE FROM Observation").executeUpdate();
    }

    @Test
    @DisplayName("Test retrieving collection Ids from empty DB")
    @TestSecurity(user = "testuser", roles = {TEST_ROLE})
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
    @TestSecurity(user = "testuser", roles = {TEST_ROLE})
    public void testRetrievingCollectionIds() {
        for (int i = 1; i < 6; i++){
            Utilities.addObservationToDatabase(Utilities.COLLECTION1, Utilities.OBSERVATION1 + i);
        }

        for (int i = 6; i < 13; i++){
            Utilities.addObservationToDatabase(Utilities.COLLECTION2, Utilities.OBSERVATION2 + i);
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

        assert(names.size() == 2);
        assert(names.contains(Utilities.COLLECTION1));
        assert(names.contains(Utilities.COLLECTION2));
    }
}

package org.uksrc.archive;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.uksrc.archive.utils.Utilities;

import java.util.Arrays;
import java.util.List;

import static io.restassured.RestAssured.when;

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
    public void testRetrieveCollectionIdsFromEmptyDB() {
        String collections = when()
                .get("/collections")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .extract()
                .asString();

        assert(collections.isEmpty());
    }

    @Test
    @DisplayName("Test retrieving collection Ids")
    public void testRetrievingCollectionIds() {
        for (int i = 0; i < 5; i++){
            Utilities.addObservationToDatabase(String.valueOf(i), Utilities.COLLECTION1);
        }

        for (int i = 5; i < 12; i++){
            Utilities.addObservationToDatabase(String.valueOf(i), Utilities.COLLECTION2);
        }

        String collections = when()
                .get("/collections")
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

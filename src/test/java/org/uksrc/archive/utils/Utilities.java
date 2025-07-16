package org.uksrc.archive.utils;

import org.ivoa.dm.caom2.DerivedObservation;
import org.ivoa.dm.caom2.Observation;
import org.ivoa.dm.caom2.ObservationIntentType;
import org.ivoa.dm.caom2.SimpleObservation;

import java.util.List;
import java.util.UUID;

public class Utilities {

    public static final String COLLECTION1 = "e-merlin";
    public static final String COLLECTION2 = "testCollection";
    public static final String OBSERVATION1 = "c630c66f-b06b-4fed-bc16-1d7fd321";
    public static final String OBSERVATION2 = "c630c66f-b06b-4fed-bc16-1d7fd321";

    // Used in testing only, must conform to the @RolesAllowed setting in the APIs
    public static final String TEST_READER_ROLE = "UKSRC/archive-service/viewer";
    public static final String TEST_WRITER_ROLE = "UKSRC/archive-service/maintainer";

    /**
     * Creates a SimpleObservation with the supplied observationId & collectionId
     * @param observationId identifier for the individual observation
     * @param collectionId identifier for the collection to add this observation to.
     * @return Observation
     */
    public static Observation createSimpleObservation(String observationId, String collectionId) {
        SimpleObservation obs = new SimpleObservation();
        obs.setId(observationId);
        obs.setCollection(collectionId);
        obs.setUriBucket("bucket");
        obs.setUri("c630c66f-b06b-4fed-bc16-1d7fd32161");
        obs.setIntent(ObservationIntentType.SCIENCE);
        return obs;
    }

    /**
     * Creates a DerivedObservation with the supplied observationId & collectionId
     * @param observationId identifier for the individual observation
     * @param collectionId identifier for the collection to add this observation to.
     * @return Observation
     */
    public static Observation createDerivedObservation(String observationId, String collectionId) {
        DerivedObservation obs = new DerivedObservation();
        obs.setId(observationId);
        obs.setCollection(collectionId);
        obs.setUriBucket("bucket");
        obs.setUri(UUID.randomUUID().toString());
        obs.setIntent(ObservationIntentType.SCIENCE);
        obs.setMembers(List.of("someone"));
        return obs;
    }
}

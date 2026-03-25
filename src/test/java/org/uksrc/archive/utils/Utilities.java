package org.uksrc.archive.utils;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.Unmarshaller;
import org.ivoa.dm.caom2.*;

import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Utilities {

    public static final String COLLECTION1 = "e-merlin";
    public static final String COLLECTION2 = "testCollection";
    public static final String OBSERVATION1 = "c630c66f-b06b-4fed-bc16-1d7fd321";
    public static final String OBSERVATION2 = "c630c66f-b06b-4fed-bc16-2e4df256";

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

    /**
     * Creates an observation with a single plane and a single artifact.
     * @param observationId identifier for the individual observation
     * @param collectionId identifier for the collection to add this observation to.
     * @param artifactUri uri of a resource (resolvable or not depending on the test in question)
     * @return An Observation with a single Artifact
     */
    public static Observation createArtifactObservation(String observationId, String collectionId, String artifactUri) {
        Observation obs = createSimpleObservation(observationId, collectionId);

        List<Artifact> artifacts = new ArrayList<>();
        Artifact artifact = new Artifact();
        artifact.setId("2cf99e88-90e1-4fe8-a502-e5cafdc6ffa1");
        artifact.setUri(artifactUri);
        artifact.setUriBucket("8ea");
        artifact.setProductType("auxiliary");
        artifact.setReleaseType(ReleaseType.DATA);
        artifact.setContentType("image/png");
        artifact.setContentLength(35205);
        artifact.setContentChecksum("md5:94624c5a190467e2fe2c1ef7cbd187ee");
        artifacts.add(artifact);

        Plane plane = new Plane();
        plane.setId(UUID.randomUUID().toString());
        plane.setUri(UUID.randomUUID().toString());
        plane.setArtifacts(artifacts);

        obs.addToPlanes(plane);

        return obs;
    }

    /**
     * Reads an XML string into a JAXB object.
     * @param xml The XML string to read.
     * @param clazz The class to unmarshal into.
     * @return The unmarshalled object.
     * @throws Exception If the XML cannot be unmarshalled.
     */
    public static  <T> T readXmlString(String xml, Class<T> clazz) throws Exception {
        JAXBContext ctx = JAXBContext.newInstance(clazz);
        Unmarshaller um = ctx.createUnmarshaller();

        Object result = um.unmarshal(new StringReader(xml));

        return (result instanceof JAXBElement<?> el)
                ? clazz.cast(el.getValue())
                : clazz.cast(result);
    }

    /**
     * Reads an XML file into a JAXB object.
     * @param path The path to the XML file.
     * @param clazz The class to unmarshal into.
     * @return The unmarshalled object.
     * @throws Exception If the XML cannot be unmarshalled.
     */
    public static <T> T readXmlFile(String path, Class<T> clazz) throws Exception {
        String xml = Files.readString(Paths.get(path));
        return readXmlString(xml, clazz);
    }
}

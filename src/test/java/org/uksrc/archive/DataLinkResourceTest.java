package org.uksrc.archive;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import org.ivoa.dm.caom2.Observation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.uksrc.archive.utils.Utilities.*;
import static org.uksrc.archive.utils.Utilities.COLLECTION1;
import static org.uksrc.archive.utils.Utilities.OBSERVATION1;

@QuarkusTest
public class DataLinkResourceTest {

    @Inject
    EntityManager em;

    @Inject
    ObservationResource observationResource;

    @Inject
    DataLinkResource dataLinkResource;

    static final String nonResolvableArtifactUri = "uri:TS8004_C_001_20190801_avg_uvplt_a_1331+3030.png";

    @BeforeEach
    @Transactional
    public void clearDatabase() {
        // Clear the table(s)
        em.createQuery("DELETE FROM Artifact").executeUpdate();
        em.createQuery("DELETE FROM Plane").executeUpdate();
        em.createQuery("DELETE FROM Observation").executeUpdate();
    }

    @Test
    @DisplayName("Add an observation with a single artifact and check the response is the same.")
    @TestSecurity(user = "testuser", roles = {TEST_READER_ROLE, TEST_WRITER_ROLE})
    public void testGettingArtifactObservation() {
        Observation obs1 = createArtifactObservation(OBSERVATION1, COLLECTION1, nonResolvableArtifactUri);

        try(Response res1 = observationResource.addObservation(obs1)) {
            assertEquals(Response.Status.CREATED.getStatusCode(), res1.getStatus());

            Response res = dataLinkResource.getDataLinkObject(OBSERVATION1);
            assertEquals(Response.Status.OK.getStatusCode(), res.getStatus());

            StreamingOutput so = (StreamingOutput) res.getEntity();

            // Capture the output into a byte array
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            so.write(baos);
            String xml = baos.toString();

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            Document doc = factory.newDocumentBuilder()
                    .parse(new java.io.ByteArrayInputStream(xml.getBytes()));

            String ns = "http://www.ivoa.net/xml/VOTable/v1.3";
            NodeList tableDataList = doc.getElementsByTagNameNS(ns, "TABLEDATA");
            assertNotEquals(0, tableDataList.getLength());

            //Find the 1st row in the TABLEDATA element
            Element tableData = (Element) tableDataList.item(0);
            NodeList rows = tableData.getElementsByTagNameNS(ns, "TR");
            assertNotEquals(0, rows.getLength());

            //Query nominated cells in the row to compare input/output values for an Artifact.
            Element row = (Element) rows.item(0);
            NodeList cells = row.getElementsByTagNameNS(ns, "TD");
            assert(cells.getLength() > 6);

            //NOTE: values need to be the same as in the object created in Utilities.createArtifactObservation()
            String value = cells.item(0).getTextContent();
            assertEquals("2cf99e88-90e1-4fe8-a502-e5cafdc6ffa1", value);

            value = cells.item(6).getTextContent();
            assertEquals("image/png", value);

        } catch (IOException | SAXException | ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("Test getting a resource that is missing from the file system.")
    @TestSecurity(user = "testuser", roles = {TEST_READER_ROLE, TEST_WRITER_ROLE})
    public void testRetrievingMissingResource() {
        Observation obs1 = createArtifactObservation(OBSERVATION1, COLLECTION1, nonResolvableArtifactUri);
        try(Response res1 = observationResource.addObservation(obs1)) {
            assertEquals(Response.Status.CREATED.getStatusCode(), res1.getStatus());

            Response res = dataLinkResource.getResource("2cf99e88-90e1-4fe8-a502-e5cafdc6ffa1");
            assertEquals(Response.Status.NOT_FOUND.getStatusCode(), res.getStatus());

            String errorMessage = res.readEntity(String.class);
            assert(errorMessage.contains("Associated resource not found for"));
        }
    }

    @Test
    @DisplayName("Test getting a resource from an Artifact that doesn't exist.")
    @TestSecurity(user = "testuser", roles = {TEST_READER_ROLE, TEST_WRITER_ROLE})
    public void testRetrievingMissingArtifact() {
        Response res = dataLinkResource.getResource("2cf99e88-90e1-4fe8-a502-1111111111");
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), res.getStatus());

        String errorMessage = res.readEntity(String.class);
        assert(errorMessage.contains("not found"));
    }

    @Test
    @DisplayName("Test resolving a local file")
    @TestSecurity(user = "testuser", roles = {TEST_READER_ROLE, TEST_WRITER_ROLE})
    public void testResolvingLocalFile() {
        //Create a temp file on the system for testing
        Path tempFile = null;
        try {
            tempFile = Files.createTempFile("test-file", ".txt");
            Files.writeString(tempFile, "Some dummy data");
            String fileUri = tempFile.toUri().toString();

            Observation obs1 = createArtifactObservation(OBSERVATION1, COLLECTION1, fileUri);
            try(Response res1 = observationResource.addObservation(obs1)) {
                assertEquals(Response.Status.CREATED.getStatusCode(), res1.getStatus());

                Response res = dataLinkResource.getResource("2cf99e88-90e1-4fe8-a502-e5cafdc6ffa1");
                assertEquals(Response.Status.OK.getStatusCode(), res.getStatus());

                ByteArrayOutputStream out = new ByteArrayOutputStream();
                ((StreamingOutput) res.getEntity()).write(out);

                assertEquals("Some dummy data", out.toString(StandardCharsets.UTF_8));
            }
        } catch (IOException e) {
            assert(false);
        }
    }
}

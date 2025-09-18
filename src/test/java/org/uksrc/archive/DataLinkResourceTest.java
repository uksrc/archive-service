package org.uksrc.archive;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import org.ivoa.dm.caom2.Artifact;
import org.ivoa.dm.caom2.Observation;
import org.ivoa.dm.caom2.Plane;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.uksrc.archive.utils.ObservationListWrapper;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

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
    public void testGettingArtifactObservation2() {
        Observation obs1 = createArtifactObservation(OBSERVATION1, COLLECTION1);

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

            Element tableData = (Element) tableDataList.item(0);
            NodeList rows = tableData.getElementsByTagNameNS(ns, "TR");
            assertNotEquals(0, rows.getLength());

            Element row = (Element) rows.item(0);
            NodeList cells = row.getElementsByTagNameNS(ns, "TD");
            assert(cells.getLength() > 6);

            String value = cells.item(0).getTextContent();
            assertEquals(value, "2cf99e88-90e1-4fe8-a502-e5cafdc6ffa1");

            value = cells.item(6).getTextContent();
            assertEquals("image/png", value);

        } catch (IOException | SAXException | ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
    }
}

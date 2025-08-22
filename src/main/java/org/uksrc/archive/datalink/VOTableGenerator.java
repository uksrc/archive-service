package org.uksrc.archive.datalink;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.StreamingOutput;
import org.ivoa.dm.caom2.Artifact;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.List;

@ApplicationScoped
public class VOTableGenerator {

    @PersistenceContext
    protected EntityManager em;

    private static final String TEMPLATE = "/templates/datalink/votable-template.xml";

    public StreamingOutput createDocument(){
        try {
            Document doc = readTemplate();

            // Find <DATA> element
            NodeList dataNodes = doc.getElementsByTagName("DATA");
            Element dataEl = (Element) dataNodes.item(0);

            // Create new <TABLEDATA>
            Element tableData = doc.createElement("TABLEDATA");

            List<ArtifactDetails> obsArtifacts = findArtifactsForObservation("2cf99e88-90e1-4fe8-a502-e5cafdc6ffa1");

            // Add rows (one for each Artifact initially)
            for (ArtifactDetails details : obsArtifacts) {
                Element tr = doc.createElement("TR");
                Artifact artifact = details.artifact();
                ArchiveTableRow row = new ArchiveTableRow(artifact.getId(), artifact.getProductType(), artifact.getUri(), null, null, details.planeId());
                row.setContentType(artifact.getContentType());
                row.setContentLength(Long.valueOf(artifact.getContentLength()));
                if (artifact.getDescriptionID() != null) {
                    row.setDescription(details.description());
                }
                addRow(doc, tr, row);
                tableData.appendChild(tr);
            }

            dataEl.appendChild(tableData);

            return out -> {
                try {
                    Transformer tf = TransformerFactory.newInstance().newTransformer();
                    tf.setOutputProperty(OutputKeys.INDENT, "yes");

                    tf.transform(new DOMSource(doc), new StreamResult(out));
                } catch (TransformerException e){
                    throw new WebApplicationException("Error streaming VOTable", e);
                }
            };
        } catch (Exception e){
            //TODO
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Adds a Document row to the supplied parent containing details from the supplied DataLink row.
     * @param doc The Document model itself
     * @param parent The parent element
     * @param row The data to display
     * @throws Exception if the getter cannot be found (for a property in the Datalink row).
     */
    private void addRow(Document doc, Element parent, DataLinkRow row) throws Exception {
        for (String fieldName : row.getFieldOrder()) {
            String getterName = "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);

            Method getter = row.getClass().getMethod(getterName);
            Object val = getter.invoke(row);

            Element td = doc.createElement("TD");
            if (val != null) {
                td.setTextContent(val.toString());
            }
            parent.appendChild(td);
        }
    }

    /**
     * Reads the template file and creates a document that can be added to.
     * @return Document that reflects the template's contents
     */
    private Document readTemplate() {
        try (InputStream is = getClass().getResourceAsStream(TEMPLATE)) {
            if (is == null) {
                throw new IllegalStateException("Template not found: " + TEMPLATE);
            }

            var dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            var db = dbf.newDocumentBuilder();

            Document doc = db.parse(is);
            doc.getDocumentElement().normalize();
            return doc;
        } catch (Exception e) {
            throw new RuntimeException("Failed to load VOTable template", e);
        }
    }

    /**
     * Get all the artifacts for the given observation ID.
     * @param observationId The ID of the observation to return all the observations for.
     * @return A List of objects that contain Artifact details along with which Plane they belong to.
     */
    public List<ArtifactDetails> findArtifactsForObservation(String observationId) {
        return em.createQuery(
                        "SELECT new org.uksrc.archive.datalink.ArtifactDetails(p.id, a, ad.description) " +
                                "FROM Observation o " +
                                "JOIN o.planes p " +
                                "JOIN p.artifacts a " +
                                "LEFT JOIN ArtifactDescription ad ON ad.uri = a.descriptionID " +
                                "WHERE o.id = :obsId", ArtifactDetails.class)
                .setParameter("obsId", observationId)
                .getResultList();
    }
}

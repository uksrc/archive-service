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

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class VOTableGenerator {

    @PersistenceContext
    protected EntityManager em;

    private static final String TEMPLATE = "/templates/datalink/votable-template.xml";

    public StreamingOutput createDocument(String observationId){
        try {
            Document doc = createVOTableDoc();

            NodeList dataNodes = doc.getElementsByTagName("RESOURCE");
            Element resEl = (Element) dataNodes.item(0);
            Element table = doc.createElement("TABLE");
            resEl.appendChild(table);

            ArtifactTableRow structure = new ArtifactTableRow(null, null, null, "TEST", null, null);
            buildTableFields(doc, table, FieldOrder.getAllFieldsOrder());

            // Create new <TABLEDATA>
            Element dataEl = doc.createElement("DATA");
            table.appendChild(dataEl);
            Element tableData = doc.createElement("TABLEDATA");

            List<ArtifactDetails> obsArtifacts = findArtifactsForObservation(observationId);

            // Add rows (one for each Artifact initially)
            for (ArtifactDetails details : obsArtifacts) {
                Element tr = doc.createElement("TR");
                Artifact artifact = details.artifact();
                ArtifactTableRow row = new ArtifactTableRow(artifact.getId(), artifact.getProductType(), artifact.getUri(), null, null, details.planeId());
                row.setContentType(artifact.getContentType());
                row.setContentLength(Long.valueOf(artifact.getContentLength()));
                if (artifact.getDescriptionID() != null) {
                    row.setDescription(details.description());
                }
                addRow(doc, tr, row, true);
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
     * @param incOptional Whether to include the optional properties.
     * @throws Exception if the getter cannot be found (for a property in the Datalink row).
     */
    private void addRow(Document doc, Element parent, DataLinkRow row, boolean incOptional) throws Exception {
        List<String> displayable = FieldOrder.getAllFieldsOrder();
        for (String fieldName : displayable) {
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
     * Adds a field to the table header, allows for optional and custom fields.
     * @param doc The Document model itself
     * @param tableEl The DOM table element
     * @param fieldDetails The actual data to add to the field.
     */
    private void addField(Document doc, Element tableEl, FieldDetails fieldDetails) {
        Element newField = doc.createElement("FIELD");
        newField.setAttribute("name", fieldDetails.name());
        newField.setAttribute("datatype", fieldDetails.dataType());
        newField.setAttribute("arraysize", fieldDetails.arraySize());
        newField.setAttribute("ucd",  fieldDetails.ucd());
        tableEl.appendChild(newField);
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
    private List<ArtifactDetails> findArtifactsForObservation(String observationId) {
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

    /**
     * Returns details of optionalFields, only displayed if required.
     * TODO - determine best approach to enable/disable (link option probably on by default anyway)
     * @return A List of FieldDetails, that can be added to the output if required.
     */
    /*private List<FieldDetails> optionalFields(){
        List<FieldDetails> fieldDetails = new ArrayList<>();
        fieldDetails.add(new FieldDetails("content_qualifier", "meta.code.qual", "char", "*", null));
        fieldDetails.add(new FieldDetails("local_semantics", "meta.code", "char", "*", null));
        fieldDetails.add(new FieldDetails("link_auth", "meta.code.auth", "char", "*", null));
        fieldDetails.add(new FieldDetails("link_authorized",  "meta.code.auth;meta.status", "char", "*", null));

        return fieldDetails;
    }*/

    /**
     * Add any custom fields that are required for the Archive Service, these are specialised fields that are not
     * required by standard DataLink outputs.
     * @return A List of FieldDetails containing information about a custom field.
     */
    private List<FieldDetails> customFields(){
        List<FieldDetails> fieldDetails = new ArrayList<>();
        fieldDetails.add(new FieldDetails("plane_id", "meta.id;meta.id.assoc", "char", "*", null));

        return fieldDetails;
    }

    private Document createVOTableDoc() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.newDocument();

        // Root <VOTABLE> element
        Element votable = doc.createElementNS("http://www.ivoa.net/xml/VOTable/v1.3", "VOTABLE");
        votable.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
        votable.setAttribute("xmlns:stc", "http://www.ivoa.net/xml/STC/v1.30");
        votable.setAttribute("version", "1.3");
        doc.appendChild(votable);

        // <RESOURCE> element
        Element resource = doc.createElement("RESOURCE");
        resource.setAttribute("type", "results");
        votable.appendChild(resource);

        return doc;
    }

    private void buildTableFields(Document doc, Element tableEl, List<String> fieldOrder) {
        for (String fieldKey : fieldOrder) {
            FieldDetails def = DataLinkFields.get(fieldKey);
            if (def != null) {
                addField(doc, tableEl, def);
            } else {
                // Optional: warn or throw if a field is missing from registry
            }
        }
    }

}

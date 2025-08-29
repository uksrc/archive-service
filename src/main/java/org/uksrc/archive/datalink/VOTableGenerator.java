package org.uksrc.archive.datalink;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.StreamingOutput;
import org.ivoa.dm.caom2.Artifact;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.lang.reflect.Method;
import java.util.List;

@ApplicationScoped
public class VOTableGenerator {

    @PersistenceContext
    protected EntityManager em;

    private static final String TEMPLATE = "/templates/datalink/votable-template.xml";

    public StreamingOutput createDocument(String observationId){
        try {
            Document doc = createVOTableDoc();

            //<RESOURCE>
            NodeList dataNodes = doc.getElementsByTagName("RESOURCE");
            Element resEl = (Element) dataNodes.item(0);
            //<TABLE>
            Element table = doc.createElement("TABLE");
            resEl.appendChild(table);

            buildTableFields(doc, table, FieldOrder.FIELD_ORDER);
            buildTableFields(doc, table, FieldOrder.OPTIONAL_FIELD_ORDER); //TODO are all required for our service?
            Comment comment = doc.createComment("Custom properties for this service");
            table.appendChild(comment);

            buildTableFields(doc, table, FieldOrder.CUSTOM_FIELD_ORDER);

            // <DATA>
            Element dataEl = doc.createElement("DATA");
            table.appendChild(dataEl);

            // <TABLEDATA>
            addResources(doc, dataEl, observationId);

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
     * Get all the artifacts for the given observation ID.
     * @param observationId The ID of the observation to return all the observations for.
     * @return A List of objects that contain Artifact details along with which Plane they belong to.
     */
    private List<ArtifactDetails> findArtifactsForObservation(String observationId) {
        return em.createQuery(
                        "SELECT new org.uksrc.archive.datalink.ArtifactDetails(a, p.id, ad.description) " +
                                "FROM Observation o " +
                                "JOIN o.planes p " +
                                "JOIN p.artifacts a " +
                                "LEFT JOIN ArtifactDescription ad ON ad.uri = a.descriptionID " +
                                "WHERE o.id = :obsId", ArtifactDetails.class)
                .setParameter("obsId", observationId)
                .getResultList();
    }

    /**
     * Creates the header structure of the XML Document.
     * @return Document that contains the basic structure.
     * @throws ParserConfigurationException if the Document cannot be created.
     */
    private Document createVOTableDoc() throws ParserConfigurationException {
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

    /**
     * Adds the FIELDs to the TABLE that define the TABLEDATA columns.
     * @param doc The XML Document that is being constructed.
     * @param tableEl The TABLE element to add to.
     * @param fieldOrder The order in which to add fields (to enforce table rows matching)
     */
    private void buildTableFields(Document doc, Element tableEl, List<String> fieldOrder) throws Exception {
        for (String fieldKey : fieldOrder) {
            FieldDetails def = DataLinkFields.get(fieldKey);
            if (def != null) {
                addField(doc, tableEl, def);
            } else {
                throw new Exception("DataLinkField " + fieldKey + " not found");
            }
        }
    }

    /**
     * Adds resource(s) to the table for the Observation supplied.
     * @param doc XML Document to add the data to.
     * @param parent The Parent element in the XML
     * @param observationId The Observation.id to extract the resources from.
     */
    private void addResources(Document doc, Element parent, String observationId){
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
            try {
                addRow(doc, tr, row, true);
            } catch (Exception e) {
                //TODO - log? stop?
                throw new RuntimeException(e);
            }
            tableData.appendChild(tr);
        }
        parent.appendChild(tableData);
    }
}

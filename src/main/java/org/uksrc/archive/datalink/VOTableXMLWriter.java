package org.uksrc.archive.datalink;

import org.ivoa.dm.caom2.Artifact;
import org.jboss.logging.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.lang.reflect.Method;
import java.util.List;

/**
 * All the functionality required to construct the VOTable required for a DataLink object.
 * Document->Fields->Resources (or error)
 * @see <a href="../../../../../../../detail.md">details.md</a> for an example structure.
 */
public class VOTableXMLWriter {

    final Logger logger;

    /**
     * Types of errors supported by IVOA DataLink
     *  @see <a href="https://www.ivoa.net/documents/DataLink/20231215/REC-DataLink-1.1.html#tth_sEc3.4">...</a>
     */
    public enum ErrorType {
        NotFoundFault,
        UsageFault,
        TransientFault,
        FatalFault,
        DefaultFault
    }

    public VOTableXMLWriter() {
        logger = Logger.getLogger(VOTableXMLWriter.class);
    }

    /**
     * Creates the header structure of the XML Document.
     * @return Document that contains the basic structure.
     * @throws ParserConfigurationException if the Document cannot be created.
     */
    public Document createVOTableDoc() throws ParserConfigurationException {
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
     * @see DataLinkFields
     */
    public void addTableFields(Document doc, Element tableEl, List<String> fieldOrder) throws Exception {
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
     * @param hostPath The host used in the access_url
     * @param resources The list of resources to add to the form.
     */
    public void addResources(Document doc, Element parent, String hostPath, List<ArtifactDetails> resources){
        // Add rows (one for each Artifact initially)
        for (ArtifactDetails details : resources) {
            Element tr = doc.createElement("TR");
            Artifact artifact = details.artifact();
            //Resolvable URI to the actual resource
            ArtifactTableRow row;
            //Only output a valid access_url if all the component parts are there.
            if (isValidArtifact(hostPath, artifact)) {
                row = createArtifactTableRow(hostPath, details);
            }
            else {
                String id = artifact != null  ? artifact.getId() : "";
                row = new ArtifactTableRow(id, null, null, null, ErrorType.FatalFault + ": unable to construct access_url for this resource", details.planeId());
            }
            try {
                addRow(doc, tr, row);
            } catch (Exception e) {
                logger.error("DataLink: Error whilst attempting to construct resource row for Artifact ID = " + (artifact != null ? artifact.getId() : "{missing}"), e);
            }
            parent.appendChild(tr);
        }
    }

    /**
     * Outputs an error message
     * @param doc XML Document to add the data to.
     * @param parent The Parent element in the XML
     * @param observationId The Observation.id that is currently being requested.
     * @param type The IVOA error type @see ErrorType
     * @param message The human-readable error message
     */
    public void addError(Document doc, Element parent, String observationId, ErrorType type, String message){
        ArtifactTableRow row = new ArtifactTableRow(observationId, null, null, null, type.toString() + ": " + message, null);
        Element tr = doc.createElement("TR");
        try {
            addRow(doc, tr, row);
        } catch (Exception e) {
            logger.error("DataLink: Error whilst attempting to construct an error row for message: " + message, e);
        }
        parent.appendChild(tr);
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
     * Adds a Document row to the supplied parent containing details from the supplied DataLink row.
     * @param doc The Document model itself
     * @param parent The parent element
     * @param row The data to display
     * @throws Exception if the getter cannot be found (for a property in the Datalink row).
     */
    private void addRow(Document doc, Element parent, DataLinkRow row) throws Exception {
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
     * Create a Document table row for an Artifact
     * @param hostPath The hostpath of the service deployment
     * @param artifactDetails Actual details of the Artifact
     * @return ArtifactTableRow for adding to a document via addRow(~)
     */
    private ArtifactTableRow createArtifactTableRow(String hostPath, ArtifactDetails artifactDetails) {
        Artifact artifact = artifactDetails.artifact();
        String accessUrl = hostPath + "/" + artifact.getId();

        ArtifactTableRow row = new ArtifactTableRow(artifact.getId(), artifact.getProductType(), accessUrl, null, null, artifactDetails.planeId());
        row.setContentType(artifact.getContentType());
        row.setContentLength(Long.valueOf(artifact.getContentLength()));
        if (artifact.getDescriptionID() != null) {
            row.setDescription(artifactDetails.description());
        }
        row.setLinkAuth("true");        //ALL resources require IAM access
        row.setLinkAuthorized("false"); //Adjust if we decide to receive current user's permissions

        return row;
    }

    /**
     * Checks if a String is either null or empty
     * @param s String to check
     * @return true if neither null nor empty.
     */
    private boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    /**
     * Checks the validity of an Artifact before adding it to the Document
     * @param hostPath expected output path of the service/resource
     * @param artifact The Artifact to test
     * @return true if suitable to add the Document
     */
    private boolean isValidArtifact(String hostPath, Artifact artifact) {
        return notBlank(hostPath)
                && artifact != null
                && notBlank(artifact.getId())
                && notBlank(artifact.getUri());
    }

}

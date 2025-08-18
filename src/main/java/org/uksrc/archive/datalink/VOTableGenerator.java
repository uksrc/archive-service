package org.uksrc.archive.datalink;

import jakarta.enterprise.context.ApplicationScoped;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.InputStream;

@ApplicationScoped
public class VOTableGenerator {

    private static final String TEMPLATE = "/templates/datalink/votable-template.xml";

    public void createDocument(){
        try {
            Document doc = readTemplate();

            Element root = doc.getDocumentElement();

            // Find <DATA> element
            NodeList dataNodes = doc.getElementsByTagName("DATA");
            Element dataEl = (Element) dataNodes.item(0);

            // Create new <TABLEDATA>
            Element tableData = doc.createElement("TABLEDATA");

            // Add rows (one for each Artifact initially)
            VOTableRow row = new VOTableRow.Builder("ID1", "auxiliary")
                    .accessUrl("https://achive.org/files/ID1/plane1/artifact1").build();
            addRow(doc, tableData, row);

            dataEl.appendChild(tableData);

            Transformer tf = TransformerFactory.newInstance().newTransformer();
            tf.setOutputProperty(OutputKeys.INDENT, "yes");
            tf.transform(new DOMSource(doc), new StreamResult(new File("output.xml")));
        } catch (Exception e){
            //TODO
            e.printStackTrace();
        }
    }

    //TODO handle null values
    private void addRow(Document doc, Element tableData, VOTableRow row) {
        Element tr = doc.createElement("TR");
        addCell(doc, tr, row.getId());        //ID (potentially something simlar to -> ivo://your.org/obs/<observation-id>/<plane-id>/<artifact-id> so that Plane info isn't lost)
        addCell(doc, tr, row.getAccessUrl()); //access_url (maybe - https://achive-service.org/files/{observation.uri}/{plane.id}/{artifact.id})
        addCell(doc, tr, row.getServiceDef()); //service_def (if defining a service for cut-outs etc that require params)
        addCell(doc, tr, row.getErrorMessage()); //error_message (identifer not found etc.)
        addCell(doc, tr, row.getDescription()); //description (Artifact.descriptionId -> ArtifactDescription.description)
        addCell(doc, tr, row.getSemantics()); //semantics (Artifact.productType)
        addCell(doc, tr, row.getContentType()); //content_type (Artifact.contentType)
        addCell(doc, tr, row.getContentLength().toString()); //content_length (Artifact.contentLength)

        tableData.appendChild(tr);
    }

    private void addCell(Document doc, Element tr, String text){
        Element td = doc.createElement("TD");
        if (text != null) {
            td.setTextContent(text);
        }
        tr.appendChild(td);
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

            return db.parse(is);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load VOTable template", e);
        }
    }
}

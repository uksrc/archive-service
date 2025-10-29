package org.uksrc.archive.datalink;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.ivoa.dm.caom2.Observation;
import org.jboss.logging.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.util.List;

/**
 * Utility to generate a Document that represents a DataLink VOTable.
 */
@ApplicationScoped
public class VOTableGenerator {

    @PersistenceContext
    protected EntityManager em;

    @ConfigProperty(name = "datalink.service.hostpath")
    String hostpath;

    final Logger logger;
    final VOTableXMLWriter xmlGenerator;


    public VOTableGenerator() {
         logger = Logger.getLogger(VOTableGenerator.class);
         xmlGenerator = new VOTableXMLWriter();
    }

    /**
     * Creates a DataLink VOTable document, based on the resources associated with the supplied observationId
     * @param observationId The ID of the observation as identified in the database (Observation.Id)
     * @return StreamingOutput of the XML (VOTable) document.
     */
    public StreamingOutput createDocument(String observationId) {
        try {
            Document doc = buildVOTableDocument(observationId);

            return out -> {
                try {
                    Transformer tf = TransformerFactory.newInstance().newTransformer();
                    tf.setOutputProperty(OutputKeys.INDENT, "yes");
                    tf.transform(new DOMSource(doc), new StreamResult(out));
                } catch (TransformerException e) {
                    throw new WebApplicationException("Error streaming VOTable", e, Response.Status.INTERNAL_SERVER_ERROR);
                }
            };

        } catch (Exception e) {
            logger.error("DataLink: Error when constructing VOTable for observation " + observationId, e);
            throw new WebApplicationException("Failed to build VOTable", e, Response.Status.INTERNAL_SERVER_ERROR);
        }
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
     * Builds the VOTable document in the expected order.
     * @param observationId The Observation.Id of the required observation in the database.
     * @return Document containing a structured representation (VOTable) of the observation's resources.
     * @throws Exception - Errors caused whilst trying to create the Document tree.
     */
    private Document buildVOTableDocument(String observationId) throws Exception {
        Document doc = xmlGenerator.createVOTableDoc();

        Element resource = (Element) doc.getElementsByTagName("RESOURCE").item(0);
        Element table = doc.createElement("TABLE");
        resource.appendChild(table);

        // Add table fields
        xmlGenerator.addTableFields(doc, table, FieldOrder.FIELD_ORDER);
        xmlGenerator.addTableFields(doc, table, FieldOrder.OPTIONAL_FIELD_ORDER);
        table.appendChild(doc.createComment("Custom properties for this service"));
        xmlGenerator.addTableFields(doc, table, FieldOrder.CUSTOM_FIELD_ORDER);

        // Add data
        Element data = doc.createElement("DATA");
        table.appendChild(data);

        Element tableData = doc.createElement("TABLEDATA");
        data.appendChild(tableData);

        Observation obs = em.find(Observation.class, observationId);
        if (obs != null) {
            List<ArtifactDetails> artifacts = findArtifactsForObservation(observationId);
            xmlGenerator.addResources(doc, tableData, hostpath, artifacts);
        } else {
            xmlGenerator.addError(doc, tableData, observationId,
                    VOTableXMLWriter.ErrorType.NotFoundFault,
                    "Supplied ID not recognised");
        }
        return doc;
    }
}

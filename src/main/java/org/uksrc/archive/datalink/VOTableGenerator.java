package org.uksrc.archive.datalink;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.StreamingOutput;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.ivoa.dm.caom2.Observation;
import org.jboss.logging.Logger;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.util.List;

@ApplicationScoped
public class VOTableGenerator {

    @PersistenceContext
    protected EntityManager em;

    @ConfigProperty(name = "datalink.service.hostpath")
    String hostpath;

    Logger logger;
    VOTableXMLWriter xmlGenerator;


    public VOTableGenerator() {
         logger = Logger.getLogger(VOTableGenerator.class);
         xmlGenerator = new VOTableXMLWriter();
    }

    public StreamingOutput createDocument(String observationId){
        try {
            Document doc = xmlGenerator.createVOTableDoc();

            //<RESOURCE>
            NodeList dataNodes = doc.getElementsByTagName("RESOURCE");
            Element resEl = (Element) dataNodes.item(0);
            //<TABLE>
            Element table = doc.createElement("TABLE");
            resEl.appendChild(table);

            xmlGenerator.addTableFields(doc, table, FieldOrder.FIELD_ORDER);
            xmlGenerator.addTableFields(doc, table, FieldOrder.OPTIONAL_FIELD_ORDER); //TODO are all required for our service?
            Comment comment = doc.createComment("Custom properties for this service");
            table.appendChild(comment);

            xmlGenerator.addTableFields(doc, table, FieldOrder.CUSTOM_FIELD_ORDER);

            // <DATA>
            Element dataEl = doc.createElement("DATA");
            table.appendChild(dataEl);

            // <TABLEDATA>
            Observation obs = em.find(Observation.class, observationId);
            Element tableData = doc.createElement("TABLEDATA");
            if (obs != null) {
                List<ArtifactDetails> artifacts = findArtifactsForObservation(observationId);
                xmlGenerator.addResources(doc, tableData, hostpath, artifacts);
            }
            else {
                xmlGenerator.addError(doc, tableData, observationId, VOTableXMLWriter.ErrorType.NotFoundFault, "supplied ID not recognised");
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
            logger.error("DataLink: Error when constructing VOTable", e);
        }
        return null;
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
}

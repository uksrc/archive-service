package org.uksrc.archive.seed;

import io.quarkus.runtime.Startup;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.ivoa.dm.caom2.Observation;
import org.uksrc.archive.utils.tools.Tools;

import java.io.InputStream;
import java.util.List;

import static org.uksrc.archive.utils.tools.Tools.FOLDER;

/**
 *  Loads example resource XML files and loads them into the database upon startup.
 *  Allows the demonstration of the service with some example resources in place (if required).
 * <p>
 *  See /seed folder
 *  See application.properties' testdata.seed.enabled to enable/disable as required.
 */
@Startup
@ApplicationScoped
public class ResourceSeedLoader {

    @Inject
    EntityManager entityManager;

    @ConfigProperty(name = "testdata.seed.enabled", defaultValue = "false")
    boolean enabled;

    /**
     * Loads and processes XML files containing observation data from a predefined folder.
     * If enabled, this method retrieves all XML files in the specified folder, parses each
     * file into an {@code Observation} object, and adds the observation to the
     * {@code observationResource}. Any exceptions encountered during the process are
     * logged to prevent interruption of the service.
     */
    @Transactional
    void onStart(@Observes StartupEvent ev) {
        if (enabled) {
            System.out.println("Loading seed data...");

            try {
                List<String> fileNames = Tools.loadSeedFileNames();
                for (String fileName : fileNames) {
                    try (InputStream is = Thread.currentThread()
                            .getContextClassLoader()
                            .getResourceAsStream(FOLDER + "/" + fileName)) {

                        if (is == null) {
                            throw new IllegalStateException("Resource not found: " + FOLDER + "/" + fileName);
                        }
                        Observation obs = readXmlStream(is, Observation.class);

                        // Check if the ID already exists in the DB
                        if (entityManager.find(Observation.class, obs.getId()) == null) {
                            entityManager.persist(obs);
                        } else {
                            System.out.println("Skipping duplicate: " + obs.getId());
                        }
                    }
                }
            }catch (Exception e){
                System.out.println("Error loading seed data: " + e.getMessage());
            }
        }
    }

    /**
     * Unmarshal an XML stream into an object of the specified class.
     *
     * @param <T>   The type of the object to be deserialized from the XML stream.
     * @param is    The InputStream from which the XML data will be read.
     * @param clazz The Class object representing the type T into which the XML data will be deserialized.
     * @return      An instance of the specified class T populated with data from the XML stream.
     * @throws JAXBException If an error occurs during the deserialization process.
     */
    public static <T> T readXmlStream(InputStream is, Class<T> clazz) throws JAXBException {
        JAXBContext ctx = JAXBContext.newInstance(clazz);
        Unmarshaller um = ctx.createUnmarshaller();

        Object result = um.unmarshal(is);

        return (result instanceof JAXBElement<?> jaxbEl)
                ? clazz.cast(jaxbEl.getValue())
                : clazz.cast(result);
    }
}

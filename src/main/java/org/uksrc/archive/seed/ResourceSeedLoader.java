package org.uksrc.archive.seed;

import io.quarkus.arc.profile.UnlessBuildProfile;
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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

/**
 *  Loads example resource XML files and loads them into the database upon startup.
 *  Allows the demonstration of the service with some example resources in place (if required).
 * <p>
 *  See /seed folder
 *  See application.properties' testdata.seed.enabled to enable/disable as required.
 */
@Startup
@ApplicationScoped
@UnlessBuildProfile("test")     //So we don't load the seed data in test mode.
public class ResourceSeedLoader {

    @Inject
    EntityManager entityManager;


    @ConfigProperty(name = "testdata.seed.enabled", defaultValue = "false")
    boolean enabled;

    private static final String FOLDER = "seed";   // The folder to search for XML files, needs to be on the classpath.

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
                List<String> fileNames = loadSeedFileNames();
                for (String fileName : fileNames) {
                    try (InputStream is = Thread.currentThread()
                            .getContextClassLoader()
                            .getResourceAsStream("seed/" + fileName)) {

                        if (is == null) {
                            throw new IllegalStateException("Resource not found: seed/" + fileName);
                        }
                        Observation obs = readXmlStream(is, Observation.class);
                        entityManager.persist(obs);
                    }
                }
            }catch (Exception e){
                System.out.println("Error loading seed data: " + e.getMessage());
            }
        }
    }

    /**
     * Loads a list of seed file names from the "seed/manifest.txt" resource located on the classpath.
     * <p>
     * The method reads the contents of the manifest file, trims each line, and filters out empty lines
     * and lines starting with a "#" character (considered as comments). The resulting list of valid file
     * names is then returned. Throws an exception if the resource is not found or encounters an error
     * during reading.
     *
     * @return A list of non-empty, valid seed file names extracted from the manifest file.
     * @throws Exception If an error occurs while accessing or reading the resource.
     */
    private List<String> loadSeedFileNames() throws Exception {
        try (InputStream is = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream("seed/manifest.txt")) {

            if (is == null) {
                throw new IllegalStateException("seed/files.txt not found on classpath");
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(is, StandardCharsets.UTF_8))) {
                return reader.lines()
                        .map(String::trim)
                        .filter(line -> !line.isEmpty())
                        .filter(line -> !line.startsWith("#"))
                        .collect(Collectors.toList());
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

package org.uksrc.archive.seed;

import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.Unmarshaller;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.ivoa.dm.caom2.Observation;
import org.uksrc.archive.ObservationResource;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Stream;

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
    ObservationResource observationResource;

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
    @PostConstruct
    @Transactional
    void load() {
        if (enabled) {
            try {
                List<String> files = findXmlFiles(FOLDER);
                for (String file : files) {
                    Observation obs = readXmlFile(file, Observation.class);
                    observationResource.addObservation(obs);
                }
            } catch (Exception e) {
                //Warn of error to stop blocking of the service itself.
                System.out.println(e.getMessage());
            }
        }
    }

    /**
     * Finds and retrieves the relative paths of all XML files within a given folder
     * accessible through the class loader.
     *
     * @param folder The name of the folder to search for XML files. This should be a
     *               resource folder accessible within the application classpath.
     * @return A list of relative paths to the XML files found within the specified folder.
     * @throws Exception If an error occurs while accessing the resources or reading files.
     */
    public static List<String> findXmlFiles(String folder) throws Exception {

        List<String> files = new ArrayList<>();

        ClassLoader cl = Thread.currentThread().getContextClassLoader();

        Enumeration<URL> resources = cl.getResources(folder);

        while (resources.hasMoreElements()) {
            URL url = resources.nextElement();

            if (url.getProtocol().equals("file")) {
                Path path = Paths.get(url.toURI());

                try (Stream<Path> stream = Files.list(path)) {
                    stream.filter(p -> p.toString().endsWith(".xml"))
                            .forEach(p -> files.add(folder + "/" + p.getFileName()));
                }
            }
        }

        return files;
    }

    /**
     * Reads an XML file into a JAXB object.
     * @param path The path to the XML file.
     * @param clazz The class to unmarshal into.
     * @return The unmarshalled object.
     * @throws Exception If the XML cannot be unmarshalled.
     */
    public static <T> T readXmlFile(String path, Class<T> clazz) throws Exception {

        try (InputStream is = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream(path)) {

            if (is == null) {
                throw new RuntimeException("Resource not found: " + path);
            }

            JAXBContext ctx = JAXBContext.newInstance(clazz);
            Unmarshaller um = ctx.createUnmarshaller();

            Object result = um.unmarshal(is);

            return (result instanceof JAXBElement<?> el)
                    ? clazz.cast(el.getValue())
                    : clazz.cast(result);
        }
    }
}

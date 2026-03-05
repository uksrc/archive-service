package org.uksrc.archive.seed;

import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.Unmarshaller;
import org.ivoa.dm.caom2.Observation;
import org.uksrc.archive.ObservationResource;

import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Startup
@ApplicationScoped
public class ResourceSeedLoader {

    @Inject
    ObservationResource observationResource;

    //List of example resource files to load on startup
    //Hard-coded to avoid potential classpath resolution issues when iterating over files in a named folder.
    private static final List<String> FILES = List.of(
            "seed/observation-example1.xml",
            "seed/observation-example2.xml",
            "seed/observation-example3.xml",
            "seed/observation-example4.xml",
            "seed/observation-example5.xml"
    );

    @PostConstruct
    @Transactional
    void load() {
        for (String file : FILES) {
            try {
                Observation obs = readXmlFile(file, Observation.class);
                observationResource.addObservation(obs);
            } catch (Exception e) {
                //Don't block execution of the service if an example resource is missing
                System.out.println(e.getMessage());
            }
        }
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

            String xml = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            return readXmlString(xml, clazz);
        }
    }

    /**
     * Reads an XML string into a JAXB object.
     * @param xml The XML string to read.
     * @param clazz The class to unmarshal into.
     * @return The unmarshalled object.
     * @throws Exception If the XML cannot be unmarshalled.
     */
    public static  <T> T readXmlString(String xml, Class<T> clazz) throws Exception {
        JAXBContext ctx = JAXBContext.newInstance(clazz);
        Unmarshaller um = ctx.createUnmarshaller();

        Object result = um.unmarshal(new StringReader(xml));

        return (result instanceof JAXBElement<?> el)
                ? clazz.cast(el.getValue())
                : clazz.cast(result);
    }
}

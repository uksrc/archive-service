package org.uksrc.archive.utils.tools;

import jakarta.persistence.TypedQuery;
import jakarta.ws.rs.core.Response;
import jakarta.xml.bind.JAXBElement;
import org.ivoa.dm.caom2.Observation;
import org.uksrc.archive.utils.ObservationListWrapper;
import org.uksrc.archive.utils.responses.Responses;

import javax.xml.namespace.QName;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;

/**
 * <p>Utility class providing common helper methods for web requests.
 * </p>
 *
 * <p>Intended to contain supportive functionality.</p>
 */
public final class Tools {

    public static final String FOLDER = "seed";   // The folder to search for XML files, needs to be on the classpath.

    /**
     * Performs the supplied query (with or without the pagination parameters)
     * @param page zero-indexed page index
     * @param size number of entries per page
     * @param query query to perform
     * @return Response containing HTTP response code and expected body if successful.
     */
    public static Response performQuery(Integer page, Integer size, TypedQuery<Observation> query) {
        try {
            if (page != null && size != null) {
                int firstResult = page * size;
                query.setFirstResult(firstResult);
                query.setMaxResults(size);
            }

            List<Observation> observations = query.getResultList();
            ObservationListWrapper wrapper = new ObservationListWrapper(observations);

            return Response.ok(wrapper).build();
        } catch (Exception e) {
            return Responses.errorResponse(e);
        }
    }

    /**
     * Converts a List of strings to a TSV
     * @param list The list of elements to convert to a TSV string.
     * @return list of items "e-merlin{TAB char}test{TAB char}ALMA"
     */
    public static String convertListToTsv(List<String> list) {
        StringJoiner joiner = new StringJoiner("\t");

        for (String item : list) {
            joiner.add(item);
        }
        return joiner.toString();
    }

    /**
     * Forces the Name header tag, now that the specialisation type is defined in the header.
     * Converts the name to Pascal-case suitable for XML responses.
     *
     * @param observation The single observation to rename
     * @return A JAXBElement of either SimpleObservation or DerivedObservation as a <caom2:Observation...
     */
    public static Object formatObservation(Observation observation) {
        Object entity = null;
        if (observation != null) {
            entity = new JAXBElement<>(
                    new QName("http://www.opencadc.org/caom2/xml/v2.5", "Observation", "caom2"), Observation.class, observation
            );
        }
        return entity;
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
    public static List<String> loadSeedFileNames() throws Exception {
        try (InputStream is = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream(FOLDER + "/manifest.txt")) {

            if (is == null) {
                throw new IllegalStateException(FOLDER + "/files.txt not found on classpath");
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
}

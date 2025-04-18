package org.uksrc.archive.utils.tools;

import jakarta.persistence.TypedQuery;
import jakarta.ws.rs.core.Response;
import jakarta.xml.bind.JAXBElement;
import org.ivoa.dm.caom2.DerivedObservation;
import org.ivoa.dm.caom2.Observation;
import org.ivoa.dm.caom2.SimpleObservation;
import org.uksrc.archive.utils.ObservationListWrapper;
import org.uksrc.archive.utils.responses.Responses;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

/**
 * <p>Utility class providing common helper methods for web requests.
 * </p>
 *
 * <p>Intended to contain supportive functionality.</p>
 */
public final class Tools {
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
     * @return list of items "e-merlin  test    ALMA"
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
}

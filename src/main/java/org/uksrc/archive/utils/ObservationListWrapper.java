package org.uksrc.archive.utils;

import jakarta.xml.bind.annotation.*;
import org.ivoa.dm.caom2.Observation;

import java.util.ArrayList;
import java.util.List;

/**
 * A wrapper class for a List<> of Observations.
 * Allows JAXB to return a List of Observations using XML via the REST APIs and
 * allows the custom naming of headers/tags/fields.
 */
@XmlRootElement(name = "Observations")  // Root element for the list
@XmlAccessorType(XmlAccessType.FIELD)
public class ObservationListWrapper {

    @XmlElement(name = "Observation", namespace = "http://www.opencadc.org/caom2/xml/v2.5")
    private List<Observation> observations = new ArrayList<>();

    @SuppressWarnings("unused")
    public ObservationListWrapper() {
    }

    public ObservationListWrapper(List<Observation> observations) {
        this.observations = observations;
    }

    @SuppressWarnings("unused")
    public List<Observation> getObservations() {
        return observations;
    }

    @SuppressWarnings("unused")
    public void setObservations(List<Observation> observations) {
        this.observations = observations;
    }
}


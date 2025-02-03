package org.uksrc.archive.utils;

import jakarta.xml.bind.annotation.*;
import org.uksrc.archive.utils.responses.ObservationResponse;

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

    //@XmlElement(name = "Observation")  //NOTE: Use this instead of the lower one to make them all "Observation" if required
   /* @XmlElements({
            @XmlElement(name = "SimpleObservation", type = SimpleObservation.class),
            @XmlElement(name = "DerivedObservation", type = DerivedObservation.class)
    })*/
    private List<ObservationResponse> observations = new ArrayList<>();

    @SuppressWarnings("unused")
    public ObservationListWrapper() {
    }

    public ObservationListWrapper(List<ObservationResponse> observations) {
        this.observations = observations;
    }

    @SuppressWarnings("unused")
    public List<ObservationResponse> getObservations() {
        return observations;
    }

    @SuppressWarnings("unused")
    public void setObservations(List<ObservationResponse> observations) {
        this.observations = observations;
    }
}


package org.uksrc.archive.utils.responses;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElements;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.ivoa.dm.caom2.DerivedObservation;
import org.ivoa.dm.caom2.Observation;
import org.ivoa.dm.caom2.SimpleObservation;

/**
 * <p>Custom response type for Observations, as the ID is auto-generated we need a way
 * of returning the actual ID as well as the actual Observation.</p>
 */
@XmlRootElement(name = "ObservationResponse")
public class ObservationResponse {
    private Long id;
    private Observation observation;

    public ObservationResponse() {}

    public ObservationResponse(Long id, Observation observation) {
        this.id = id;
        this.observation = observation;
    }

    @XmlElement
    public Long getId() {return id;}

    public void setId(Long id) {this.id = id;}
    @XmlElements({
            @XmlElement(name = "SimpleObservation", type = SimpleObservation.class),
            @XmlElement(name = "DerivedObservation", type = DerivedObservation.class)
    })
    public Observation getObservation() {return observation;}

    public void setObservation(Observation message) {this.observation = message;}
}

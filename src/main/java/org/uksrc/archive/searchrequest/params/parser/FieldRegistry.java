package org.uksrc.archive.searchrequest.params.parser;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.Map;
import java.util.Optional;

import org.ivoa.dm.caom2.EnergyBand;
import  org.uksrc.archive.searchrequest.params.parser.DescriptorFactory.*;

@ApplicationScoped
public class FieldRegistry {

    private final Map<String, FieldDefinition> fields = Map.of(
            "project", new FieldDefinition(
                    "project",
                    "proposal.project",
                    FieldType.STRING,
                    null
            ),
            "target", new FieldDefinition(
                    "target",
                    "target.name",
                    FieldType.STRING,
                    null
            ),
            "band", new FieldDefinition(
                    "band",
                    "planes.energy.energyBands",
                    FieldType.COLLECTION,
                    EnergyBand.class
            ),
            "freq", new FieldDefinition(
                    "freq",
                    "plane.energy.bounds",
                    FieldType.SPECTRAL_RANGE,
                    null
            )
    );

    public Optional<FieldDefinition> get(String param) {
        return Optional.ofNullable(fields.get(param));
    }
}

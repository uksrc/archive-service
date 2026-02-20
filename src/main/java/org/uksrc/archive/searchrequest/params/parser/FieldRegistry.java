package org.uksrc.archive.searchrequest.params.parser;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.Map;
import java.util.Optional;

import  org.uksrc.archive.searchrequest.params.parser.DescriptorFactory.*;

@ApplicationScoped
public class FieldRegistry {

    private final Map<String, FieldDefinition> fields = Map.of(
            "project", new FieldDefinition(
                    "project",
                    "proposal.project",
                    FieldType.STRING
            ),
            "target", new FieldDefinition(
                    "target",
                    "target.name",
                    FieldType.STRING
            ),
            "band", new FieldDefinition(
                    "band",
                    "planes.energy.energyBands",
                    FieldType.COLLECTION
            ),
            "freq", new FieldDefinition(
                    "freq",
                    "planes.energy.bounds",
                    FieldType.SPECTRAL_RANGE,
                    "lower",
                    "upper"
            )
    );

    public Optional<FieldDefinition> get(String param) {
        return Optional.ofNullable(fields.get(param));
    }
}

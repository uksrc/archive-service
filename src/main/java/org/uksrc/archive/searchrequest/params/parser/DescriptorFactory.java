package org.uksrc.archive.searchrequest.params.parser;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MultivaluedMap;
import org.uksrc.archive.searchrequest.params.descriptors.CollectionDescriptor;
import org.uksrc.archive.searchrequest.params.descriptors.EqualsDescriptor;
import org.uksrc.archive.searchrequest.params.descriptors.PredicateDescriptor;
import org.uksrc.archive.searchrequest.params.descriptors.RangeDescriptor;

import java.util.*;

@ApplicationScoped
public class DescriptorFactory {

    @Inject
    FieldRegistry registry;

    public enum FieldType {
        STRING,
        NUMBER,
        RANGE,
        SPECTRAL_RANGE,
        BAND,
        ENUM,
        COLLECTION
    }

    public record FieldDefinition(
            String paramName,
            String entityPath,
            FieldType type,
            String minAttribute,
            String maxAttribute
    ) {
        // Convenience constructor for scalar values
        public FieldDefinition(String paramName, String entityPath, FieldType type) {
            this(paramName, entityPath, type, null, null);
        }
    }

    public List<PredicateDescriptor> fromQueryParams(MultivaluedMap<String, String> params) {
        List<PredicateDescriptor> descriptors = new ArrayList<>();
        Map<String, Double> minValues = new HashMap<>();
        Map<String, Double> maxValues = new HashMap<>();

        // Pass 1: Handle Scalars and Collect Range Parts
        params.forEach((key, values) -> {
            if (values == null || values.isEmpty()) return;

            if (key.endsWith("Min")) {
                minValues.put(stripSuffix(key), parse(values.get(0)));
                return;
            }
            if (key.endsWith("Max")) {
                maxValues.put(stripSuffix(key), parse(values.get(0)));
                return;
            }

            if (key.endsWith("_min")) {
                minValues.put(stripSuffix(key), parse(values.get(0)));
                return;
            }
            if (key.endsWith("_max")) {
                maxValues.put(stripSuffix(key), parse(values.get(0)));
                return;
            }

            registry.get(key).ifPresent(def -> {
                switch (def.type()) {
                    case STRING -> descriptors.add(new EqualsDescriptor<>(def.entityPath(), values.get(0)));
                    case BAND, COLLECTION -> descriptors.add(new CollectionDescriptor(def, values.get(0)));
                    // If the user sends freq=1.2 (not freq_min), you might want to treat it as a point range
                    case SPECTRAL_RANGE, RANGE -> {
                        Double val = parse(values.get(0));
                        minValues.putIfAbsent(key, val);
                        maxValues.putIfAbsent(key, val);
                    }
                }
            });
        });

        // Pass 2: Process the Range Maps
        Set<String> allRangeKeys = new HashSet<>(minValues.keySet());
        allRangeKeys.addAll(maxValues.keySet());

        for (String base : allRangeKeys) {
            registry.get(base).ifPresent(def -> {
                Double min = minValues.get(base);
                Double max = maxValues.get(base);

                if (def.type() == FieldType.SPECTRAL_RANGE || def.type() == FieldType.RANGE) {
                    // Use the generic RangeDescriptor we built
                    // It uses def.minAttribute() and def.maxAttribute() internally
                    descriptors.add(new RangeDescriptor<>(def, min, max));
                }
            });
        }

        return descriptors;
    }

    private String stripSuffix(String key) {
        final String substring = key.substring(0, key.length() - 3);

        if (key.endsWith("Min")) return substring;
        if (key.endsWith("Max")) return substring;
        return key;
    }

    private Double parse(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid numeric value: " + s, e);
        }
    }
}

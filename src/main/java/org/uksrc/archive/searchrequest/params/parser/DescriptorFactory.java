package org.uksrc.archive.searchrequest.params.parser;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MultivaluedMap;
import org.uksrc.archive.searchrequest.params.descriptors.CollectionDescriptor;
import org.uksrc.archive.searchrequest.params.descriptors.EqualsDescriptor;
import org.uksrc.archive.searchrequest.params.descriptors.PredicateDescriptor;

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
            Class<? extends Enum<?>> enumClass
    ) {}


    //   public record Range<T extends Comparable<T>>(T lower, T upper) {}

    public List<PredicateDescriptor> fromQueryParams(MultivaluedMap<String, String> params) {

        List<PredicateDescriptor> descriptors = new ArrayList<>();

        Map<String, Double> minValues = new HashMap<>();
        Map<String, Double> maxValues = new HashMap<>();

        // First pass — collect everything
        params.forEach((key, values) -> {

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

                    case STRING ->
                            descriptors.add(
                                    new EqualsDescriptor(def.entityPath(), values.get(0))
                            );
                    case ENUM, COLLECTION -> descriptors.add(
                            new CollectionDescriptor(
                                    def,
                                    values.get(0)
                            )
                    );


//                    case BAND ->
//                            descriptors.add(
//                                    new BandDescriptor(values.get(0))
//                            );
                }
            });
        });

        // Second pass — handle ranges generically
//        for (String base : union(minValues.keySet(), maxValues.keySet())) {
//
//            registry.get(base).ifPresent(def -> {
//
//                Double min = minValues.get(base);
//                Double max = maxValues.get(base);
//
//                switch (def.type()) {
//
//                    case RANGE -> descriptors.add(
//                            new IntervalOverlapDescriptor(
//                                    def.entityPath() + ".lower",
//                                    def.entityPath() + ".upper",
//                                    min,
//                                    max
//                            )
//                    );
//
//                    case SPECTRAL_RANGE -> {
//                        Range<Double> r =
//                                convertFrequencyToWavelength(min, max);
//
//                        descriptors.add(
//                                new IntervalOverlapDescriptor(
//                                        def.entityPath() + ".lower",
//                                        def.entityPath() + ".upper",
//                                        r.lower(),
//                                        r.upper()
//                                )
//                        );
//                    }
//                }
//            });
//        }

        return descriptors;
    }




    boolean isSimpleField(String key) {
        return registry.get(key)
                .map(def -> def.type() == FieldType.STRING
                        || def.type() == FieldType.NUMBER)
                .orElse(false);
    }

    private String stripSuffix(String key) {
        if (key.endsWith("_min")) {
            return key.substring(0, key.length() - 4);
        }
        if (key.endsWith("_max")) {
            return key.substring(0, key.length() - 4);
        }
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

    private Set<String> union(Set<String> a, Set<String> b) {
        Set<String> result = new HashSet<>(a);
        result.addAll(b);
        return result;
    }


}

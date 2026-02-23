package org.uksrc.archive.searchrequest.params.parser;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MultivaluedMap;
import org.uksrc.archive.searchrequest.params.descriptors.CollectionDescriptor;
import org.uksrc.archive.searchrequest.params.descriptors.EqualsDescriptor;
import org.uksrc.archive.searchrequest.params.descriptors.PredicateDescriptor;
import org.uksrc.archive.searchrequest.params.descriptors.RangeDescriptor;
import static org.uksrc.archive.searchrequest.params.parser.FieldRegistry.FieldDefinition;
import static org.uksrc.archive.searchrequest.params.parser.FieldRegistry.FieldType;

import java.util.*;

/**
 * The {@code DescriptorFactory} class is responsible for creating and managing
 * {@link PredicateDescriptor} objects based on query parameters. It processes
 * scalar values, range values, and complex field definitions to construct search
 * criteria represented as descriptors.
 * <p>
 * The factory includes methods to handle scalar values directly and generate
 * range-based descriptors by identifying minimum and maximum values from the query parameters.
 * It supports different field types such as strings, collections, bands, spectral ranges, and numeric ranges.
 * <p>
 * This class interacts with {@link FieldRegistry} to retrieve field metadata
 * and uses it to create appropriate descriptor instances.
 */
@ApplicationScoped
public class DescriptorFactory {

    @Inject
    FieldRegistry registry;

    /**
     * Converts query parameters into a list of {@code PredicateDescriptor} instances
     * that define the search criteria.
     *
     * @param params a map of query parameters where the key represents the parameter name
     *               and the value contains the list of associated values
     * @return a list of {@code PredicateDescriptor} instances derived from the query parameters
     */
    public List<PredicateDescriptor> fromQueryParams(MultivaluedMap<String, String> params) {
        List<PredicateDescriptor> descriptors = new ArrayList<>();
        Map<String, Double> minValues = new HashMap<>();
        Map<String, Double> maxValues = new HashMap<>();

        // Handle scalar values or range points (and extract range values)
        params.forEach((key, values) -> {
            if (values == null || values.isEmpty()) return;
            processParameter(key, values.get(0), descriptors, minValues, maxValues);
        });

        // Handle ranges
        descriptors.addAll(createRangeDescriptors(minValues, maxValues));

        return descriptors;
    }

    /**
     * Processes a given parameter by determining whether it represents a range (min/max) or a scalar value
     * and updates the corresponding collections or descriptors accordingly.
     *
     * @param key          the parameter key that identifies the parameter type (e.g., scalar or range).
     * @param value        the parameter value to be processed.
     * @param descriptors  the list of {@code PredicateDescriptor} objects to add new descriptors to.
     * @param minValues    a map used to store minimum values for range parameters, keyed by the stripped parameter name.
     * @param maxValues    a map used to store maximum values for range parameters, keyed by the stripped parameter name.
     */
    private void processParameter(String key, String value, List<PredicateDescriptor> descriptors,
                                  Map<String, Double> minValues, Map<String, Double> maxValues) {

        // Handle Range Suffixes
        if (isMin(key)) {
            minValues.put(stripSuffix(key), parse(value));
        } else if (isMax(key)) {
            maxValues.put(stripSuffix(key), parse(value));
        } else {
            // Handle Standalone/Point values
            registry.get(key).ifPresent(def -> handleScalarOrPoint(def, key, value, descriptors, minValues, maxValues));
        }
    }

    /**
     * Handles the processing of a given field definition and its associated value
     * by determining the appropriate descriptor or value mappings to use.
     * Depending on the field type, this method dynamically adds descriptors
     * or updates the range maps for minimum and maximum values.
     * NOTE: Also extracts values for ranges
     *
     * @param def the field definition that provides metadata, such as the field type
     *            and entity path information.
     * @param key the parameter key representing the name of the parameter being processed.
     * @param value the parameter value to be evaluated and used for creating descriptors
     *              or updating minimum and maximum values.
     * @param descriptors a list of {@code PredicateDescriptor} objects where new descriptors
     *                    will be added based on the field type and value.
     * @param minValues a map to store minimum values for range-based fields, keyed by the
     *                  stripped parameter name.
     * @param maxValues a map to store maximum values for range-based fields, keyed by the
     *                  stripped parameter name.
     */
    private void handleScalarOrPoint(FieldDefinition def, String key, String value,
                                     List<PredicateDescriptor> descriptors,
                                     Map<String, Double> minValues, Map<String, Double> maxValues) {
        switch (def.type()) {
            case STRING          -> descriptors.add(new EqualsDescriptor<>(def.entityPath(), value));
            case BAND, COLLECTION -> descriptors.add(new CollectionDescriptor(def, value));
            case SPECTRAL_RANGE, RANGE -> {
                Double val = parse(value);
                minValues.putIfAbsent(key, val);
                maxValues.putIfAbsent(key, val);
            }
        }
    }

    /**
     * Creates a list of {@code PredicateDescriptor} objects based on the provided minimum
     * and maximum value maps. For each key in the combined set of {@code minValues} and
     * {@code maxValues}, a corresponding {@code PredicateDescriptor} is generated if a
     * field definition exists in the registry.
     * NOTE: Uses the range parameter key/value pairs that were extracted from the query parameters,
     * during the iterative processing of the method {@code handleScalarOrPoint}
     *
     * @param minValues a map containing minimum values for range-based parameters, keyed by parameter name
     * @param maxValues a map containing maximum values for range-based parameters, keyed by parameter name
     * @return a list of {@code PredicateDescriptor} instances built using the provided ranges
     */
    private List<PredicateDescriptor> createRangeDescriptors(Map<String, Double> minValues, Map<String, Double> maxValues) {
        List<PredicateDescriptor> rangeDescriptors = new ArrayList<>();
        Set<String> allKeys = new HashSet<>(minValues.keySet());
        allKeys.addAll(maxValues.keySet());

        for (String key : allKeys) {
            registry.get(key).ifPresent(def -> {
                Double valMin = minValues.get(key);
                Double valMax = maxValues.get(key);
                rangeDescriptors.add(buildRangeDescriptor(def, valMin, valMax));
            });
        }
        return rangeDescriptors;
    }

    /**
     * Builds a {@code PredicateDescriptor} instance for a provided field definition and its range values.
     * This method determines the type of the field and creates an appropriate {@code RangeDescriptor}.
     * If the field type is {@code SPECTRAL_RANGE}, it performs wavelength-frequency conversion.
     *
     * @param def the {@code FieldDefinition} that provides metadata such as field type and entity path.
     * @param min the minimum value of the range, or {@code null} if no minimum is defined.
     * @param max the maximum value of the range, or {@code null} if no maximum is defined.
     * @return a {@code PredicateDescriptor} instance based on the provided field definition and range values.
     */
    private PredicateDescriptor buildRangeDescriptor(FieldRegistry.FieldDefinition def, Double min, Double max) {
        if (def.type() == FieldType.SPECTRAL_RANGE) {
            double c = 299792458.0;
            // Inverse swap: High freq = Low wavelength
            Double wavMin = (max != null && max != 0) ? c / max : null;
            Double wavMax = (min != null && min != 0) ? c / min : null;
            return new RangeDescriptor<>(def, wavMin, wavMax);
        }
        return new RangeDescriptor<>(def, min, max);
    }

// --- Helper Utilities ---

    private boolean isMin(String key) { return key.endsWith("Min") || key.endsWith("_min"); }
    private boolean isMax(String key) { return key.endsWith("Max") || key.endsWith("_max"); }

    private String stripSuffix(String key) {
        return key.replaceAll("(_min|Min|_max|Max)$", "");
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

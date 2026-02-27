package org.uksrc.archive.searchrequest.params.parser;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MultivaluedMap;
import org.uksrc.archive.searchrequest.params.descriptors.*;

import static org.uksrc.archive.searchrequest.params.parser.FieldRegistry.FieldDefinition;

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
    FieldRegistry registry;         /* Access to the registry of field definitions */

    /**
     * Converts query parameters into a list of {@code PredicateDescriptor} instances
     * that define the search criteria.
     *
     * @param params a map of query parameters where the key represents the parameter name
     *               and the value contains the list of associated values
     * @return a list of {@code PredicateDescriptor} instances derived from the query parameters
     */
    public List<PredicateDescriptor> fromQueryParams(MultivaluedMap<String, String> params) {
        validateParamKeys(params.keySet());

        List<PredicateDescriptor> descriptors = new ArrayList<>();
        Map<String, String> minValues = new HashMap<>();
        Map<String, String> maxValues = new HashMap<>();

        // Handle scalar values or range points (and extract range values)
        params.forEach((key, values) -> {
            if (values == null || values.isEmpty()) return;

            if (!isConeParameter(key)) {
                processParameter(key, values.get(0), descriptors, minValues, maxValues);
            }
        });

        // Handle ranges
        descriptors.addAll(createRangeDescriptors(minValues, maxValues));
        createConeDescriptor(params).ifPresent(descriptors::add);

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
                                  Map<String, String> minValues, Map<String, String> maxValues) {

        // Handle Range Suffixes
        if (isMin(key)) {
            minValues.put(stripSuffix(key), value);
        } else if (isMax(key)) {
            maxValues.put(stripSuffix(key), value);
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
                                     Map<String, String> minValues, Map<String, String> maxValues) {
        switch (def.type()) {
            case STRING          -> descriptors.add(new EqualsDescriptor<>(def, value));
            case BAND, COLLECTION -> descriptors.add(new CollectionDescriptor(def, value));
            case SPECTRAL_RANGE, RANGE -> {
                minValues.putIfAbsent(key, value);
                maxValues.putIfAbsent(key, value);
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
    private List<PredicateDescriptor> createRangeDescriptors(Map<String, String> minValues, Map<String, String> maxValues) {
        List<PredicateDescriptor> rangeDescriptors = new ArrayList<>();
        Set<String> allKeys = new HashSet<>(minValues.keySet());
        allKeys.addAll(maxValues.keySet());

        //Process ALL min/max key values
        for (String key : allKeys) {
            registry.get(key).ifPresent(def -> {
                String valMin = minValues.get(key);
                String valMax = maxValues.get(key);
                rangeDescriptors.add(new RangeDescriptor(def, valMin, valMax));
            });
        }
        return rangeDescriptors;
    }

    /**
     * Creates a {@code PredicateDescriptor} for a cone search query based on the provided parameters.
     * A cone search operation requires three specific parameters: "ra" (right ascension),
     * "dec" (declination), and "radius". If any one of these parameters is provided without the
     * others, an {@code IllegalArgumentException} is thrown. If all required parameters are
     * available and valid, a {@code ConeSearchDescriptor} is created. Otherwise, an empty
     * {@code Optional} is returned.
     *
     * @param params a map of query parameters where the key is the parameter name
     *               and the value is a list of associated values. The method specifically
     *               looks for the parameters "ra", "dec", and "radius".
     * @return an {@code Optional} containing a {@code PredicateDescriptor} for the cone search
     *         criteria, or an empty {@code Optional} if none of the cone search parameters are present.
     * @throws IllegalArgumentException if only a subset of the cone search parameters are provided,
     *                                  or if the parameters are not in a valid numeric format.
     */
    private Optional<PredicateDescriptor> createConeDescriptor(MultivaluedMap<String, String> params) {
        String raVal = params.getFirst("ra");
        String decVal = params.getFirst("dec");
        String radVal = params.getFirst("radius");

        //Check that all three of the cone search parameters are present
        boolean hasAny = (raVal != null || decVal != null || radVal != null);
        boolean hasAll = (raVal != null && decVal != null && radVal != null);

        if (hasAny && !hasAll) {
            throw new IllegalArgumentException(
                    "Incomplete Cone Search: 'ra', 'dec', and 'radius' must all be provided together."
            );
        }

        if (hasAll) {
            // Look up the FieldDefinition that maps to targetPosition.coordinates
            return registry.get("cone").map(def -> {
                try {
                    double ra = Double.parseDouble(raVal);
                    double dec = Double.parseDouble(decVal);
                    double radius = Double.parseDouble(radVal);
                    return new ConeSearchDescriptor(def, ra, dec, radius);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid numeric format for cone search parameters.");
                }
            });
        }
        return Optional.empty();
    }

// --- Helper Utilities ---

    //Utilities to help with parameter processing for values that are supplied with a min & max suffix.
    //Queries for frequency, for example, that expect 'freqMin' and 'freqMax'
    private boolean isMin(String key) { return key.endsWith("Min") || key.endsWith("_min"); }
    private boolean isMax(String key) { return key.endsWith("Max") || key.endsWith("_max"); }

    private String stripSuffix(String key) {
        return key.replaceAll("(_min|Min|_max|Max)$", "");
    }

    /**
     * Simple check against known cone search parameters
     * @param key parameter name
     * @return boolean true if parameter is a cone search parameter
     */
    private boolean isConeParameter(String key) {
        return "ra".equalsIgnoreCase(key) || "dec".equalsIgnoreCase(key) || "radius".equalsIgnoreCase(key);
    }

    /**
     * Validates a set of parameter keys to ensure they conform to accepted criteria.
     * A parameter key is considered valid if it is either:
     * - Registered in the parameter registry.
     * - Recognised as a known spatial parameter (e.g., "ra", "dec", "radius").
     * - A recognised global parameter for pagination (e.g., "page", "size").
     * If any key is not recognised under these conditions, this method throws an
     * {@code IllegalArgumentException}.
     *
     * @param keys the set of parameter keys to be validated
     * @throws IllegalArgumentException if an unrecognised parameter key is encountered
     */
    private void validateParamKeys(Set<String> keys) {
        for (String key : keys) {
            // A key is valid if it's in the registry OR it's a known spatial parameter
            boolean isRegistered = registry.containsParam(key);
            boolean isSpatial = isConeParameter(key);
            boolean isRange = isMin(key) || isMax(key);

            // Add other global params here (e.g., "limit", "offset", "sort")
            boolean isPagination = "limit".equalsIgnoreCase(key) || "offset".equalsIgnoreCase(key);

            if (!isRegistered && !isSpatial && !isPagination && !isRange) {
                throw new IllegalArgumentException("Unknown query parameter: " + key);
            }
        }
    }
}

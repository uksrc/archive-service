package org.uksrc.archive.searchrequest.params.parser;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.Map;
import java.util.Optional;

import  org.uksrc.archive.searchrequest.params.parser.DescriptorFactory.*;
import org.uksrc.archive.searchrequest.params.transform.RangeTransformer;
import org.uksrc.archive.searchrequest.params.transform.Transformers;

/**
 * A registry for managing field definitions that are used to describe parameters
 * and their associated metadata. This class provides a way to store and retrieve
 * information about fields, such as their names, paths, types, and optional constraints
 * like minimum and maximum attributes.
 * <p>
 * The registry relies on a predefined set of field definitions that map field names
 * to their corresponding {@link FieldDefinition} objects. These definitions provide
 * structured information about how parameters should be represented and accessed.
 * <p>
 * This class supports querying field definitions by their parameter names.
 */
@ApplicationScoped
public class FieldRegistry {

    /**
     * Represents the different types of fields that can be used to define
     * parameters in a field registry. Each field type describes the nature
     * of the data it represents, such as strings, numerical values, ranges,
     * or collections.
     */
    public enum FieldType {
        STRING,
        NUMBER,
        DATE,
        RANGE,
        SPECTRAL_RANGE,
        BAND,
        ENUM,
        COLLECTION,
        CONE
    }

    /**
     * Represents the definition of a field in a registry, providing descriptive metadata
     * used for managing parameters associated with entities and their attributes. This
     * record encapsulates details such as the parameter name, the path to its associated
     * entity, the type of the field, and optional constraints on its minimum and maximum
     * attributes.
     *
     * @param paramName      The name of the parameter associated with the field.
     * @param entityPath     The path to the entity or attribute within the domain model associated
     *                       with this field.
     * @param type           The type of the field, indicating the nature of its data. Expected
     *                       values are defined in the {@link FieldType} enumeration.
     * @param minAttribute   An optional value representing the name of the field's minimum constraint
     *                       attribute, if applicable.
     * @param maxAttribute   An optional value representing the name of the field's maximum constraint
     *                       attribute, if applicable.
     */
    public record FieldDefinition(
            String paramName,
            String entityPath,
            FieldType type,
            String minAttribute,
            String maxAttribute,
            RangeTransformer transformer
    ) {
        // Convenience constructor for scalar values
        public FieldDefinition(String paramName, String entityPath, FieldType type) {
            this(paramName, entityPath, type, null, null, null);
        }
    }

    /**
     * A map that defines metadata for fields used within the field registry. Each key represents
     * the name of a field, and its corresponding value is an instance of {@link FieldDefinition}
     * that provides descriptive information about the field, such as its path, type, and optional
     * constraints.
     * <p>
     * Field definitions included:
     * <p>
     * "project":
     *   - Represents the project associated with the proposal.
     *   - Path: "proposal.project"
     *   - Type: {@link FieldType#STRING}
     *
     * "target":
     *   - Represents the name of the target entity.
     *   - Path: "target.name"
     *   - Type: {@link FieldType#STRING}
     *
     * "band":
     *   - Represents a collection of energy bands.
     *   - Path: "planes.energy.energyBands"
     *   - Type: {@link FieldType#COLLECTION}
     *
     * "freq":
     *   - Represents the spectral range bounds.
     *   - Path: "planes.energy.bounds"
     *   - Type: {@link FieldType#SPECTRAL_RANGE}
     *   - Minimum Attribute: "lower"
     *   - Maximum Attribute: "upper"
     */
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
                    "upper",
                    Transformers.FREQUENCY_TO_WAVELENGTH
            ),
            "dateRange", new FieldDefinition(       //Uses the overlap range
                    "dateRange",
                    "planes.time.bounds",
                    FieldType.RANGE,
                    "lower",
                    "upper",
                    Transformers.ISO_TO_MJD
            ),
            "startDate", new FieldDefinition(       //Ignores the upper value (end date in this case)
                    "startDate",
                    "planes.time.bounds",
                    FieldType.RANGE,
                    "lower",
                    "upper",
                   Transformers.STRICT_START_TIME
            ),
            "cone", new FieldDefinition(
                    "cone",                      // Search key
                    "targetPosition.coordinates", // Base path to the entity
                    FieldType.CONE,              // New type
                    "cval1",                     // RA column
                    "cval2",
                    null
            )
    );

    /**
     * Retrieves the {@link FieldDefinition} associated with the given parameter name.
     * If no field definition exists for the specified parameter, an empty {@link Optional} is returned.
     *
     * @param param The name of the parameter whose associated {@link FieldDefinition} is to be retrieved.
     * @return An {@link Optional} containing the corresponding {@link FieldDefinition}, or an empty {@link Optional} if no definition is found.
     */
    public Optional<FieldDefinition> get(String param) {
        return Optional.ofNullable(fields.get(param));
    }
}

package org.uksrc.archive.searchrequest.params.parser;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.Map;
import java.util.Optional;

import  org.uksrc.archive.searchrequest.params.parser.DescriptorFactory.*;
import org.uksrc.archive.searchrequest.params.transform.Transformer;
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
        CONE,
        STRING_ARRAY
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
            Transformer transformer
    ) {
        // Convenience constructor for scalar values
        public FieldDefinition(String paramName, String entityPath, FieldType type) {
            this(paramName, entityPath, type, null, null, null);
        }
    }

    /**
     * A map representing the registered field definitions available for querying
     * parameters and their corresponding metadata in the application domain. Each
     * entry consists of a parameter name as the key and a {@link FieldDefinition}
     * as the value, which provides details such as the parameter name, path to the
     * associated entity, field type, and optional constraints or transformations.
     * <p>
     * Key details for each registered field:
     * <p>
     * - project: Represents the project field and maps to "proposal.project". Its type is {@link FieldType#STRING}.
     * - telescope: Represents the telescope field and maps to "telescope.name". Its type is {@link FieldType#STRING}.
     * - instrument: Represents the instrument field and maps to "instrument.name". Its type is {@link FieldType#STRING}.
     * - target: Represents the target field and maps to "target.name". Its type is {@link FieldType#STRING}.
     * - band: Represents the band field and maps to "planes.energy.bandpassName". Its type is {@link FieldType#STRING_ARRAY}.
     * - freq: Represents the frequency field, mapping to "planes.energy.bounds" with "lower" and "upper" constraints.
     *         The field type is {@link FieldType#SPECTRAL_RANGE} with a transformation applied: {@link Transformers#FREQUENCY_TO_WAVELENGTH}.
     *         Should be supplied as freqMin and freqMax in Hz.
     * - date: Represents the date field using an overlap range, mapping to "planes.time.bounds" with "lower" and "upper" constraints.
     *         Its type is {@link FieldType#RANGE} with a transformation applied: {@link Transformers#ISO_TO_MJD}.
     *         Should be supplied as dateMin and dateMax in ISO-8601 format.
     * - startDate: Represents the startDate field, mapping to "planes.time.bounds" while ignoring the upper constraint.
     *              Its type is {@link FieldType#DATE} and applies a transformation: {@link Transformers#ISO_TO_MJD}.
     * - cone: Represents a cone search field, mapping to "targetPosition.coordinates".
     *         The parameters include "ra" for Right Ascension (RA) and "dec" for Declination (Dec) & "radius", with a {@link FieldType#CONE}.
     */
    private final Map<String, FieldDefinition> fields = Map.of(
            "project", new FieldDefinition(
                    "project",
                    "proposal.project",
                    FieldType.STRING
            ),
            "telescope", new FieldDefinition(
                    "telescope",
                    "telescope.name",
                    FieldType.STRING
            ),
            "instrument", new FieldDefinition(
                    "instrument",
                    "instrument.name",
                    FieldType.STRING
            ),
            "target", new FieldDefinition(
                    "target",
                    "target.name",
                    FieldType.STRING
            ),
            "band", new FieldDefinition(
                    "band",
                    "planes.energy.bandpassName",
                    FieldType.STRING_ARRAY
            ),
            "freq", new FieldDefinition(
                    "freq",
                    "planes.energy.bounds",
                    FieldType.SPECTRAL_RANGE,
                    "lower",
                    "upper",
                    Transformers.FREQUENCY_TO_WAVELENGTH
            ),
            "date", new FieldDefinition(       //Uses the overlap range
                    "date",
                    "planes.time.bounds",
                    FieldType.RANGE,
                    "lower",
                    "upper",
                    Transformers.ISO_TO_MJD
            ),
            "startDate", new FieldDefinition(       //Ignores the upper value (end date in this case)
                    "startDate",
                    "planes.time.bounds",
                    FieldType.DATE,
                    "lower",
                    "upper",
                   Transformers.ISO_TO_MJD
            ),
            "cone", new FieldDefinition(
                    "cone",             // Search key
                    "targetPosition.coordinates",   // Base path to the entity
                    FieldType.CONE,
                    "cval1",            // RA column
                    "cval2",                        // Dec column
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

    /**
     * Checks if the specified parameter name exists in the registry.
     *
     * @param param The name of the parameter to check for existence.
     * @return {@code true} if the parameter exists in the registry, {@code false} otherwise.
     */
    public boolean containsParam(String param) {
        return fields.containsKey(param);
    }
}

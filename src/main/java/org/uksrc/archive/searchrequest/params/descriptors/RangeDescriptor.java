package org.uksrc.archive.searchrequest.params.descriptors;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import org.uksrc.archive.searchrequest.query.QueryContext;
import static org.uksrc.archive.searchrequest.params.parser.FieldRegistry.FieldDefinition;

/**
 * Represents a descriptor for defining range-based search criteria in JPA queries. This class
 * builds a predicate that matches entities where a specified range (defined by minimum and maximum values)
 * overlaps with the range defined in the query.
 *
 * @param <V> The type of the range values, which must implement {@link Comparable}.
 */
public class RangeDescriptor<V extends Comparable<? super V>>
        implements PredicateDescriptor {

    private final FieldDefinition fieldDef;
    private final V min;
    private final V max;

    /**
     * Constructs a new RangeDescriptor instance for defining a range-based search criterion.
     *
     * @param fieldDef The field definition that specifies the metadata of the entity attribute
     *                 used for range-based comparison. It includes the entity path and optional
     *                 minimum and maximum attribute names.
     * @param min      The minimum value of the range. Entities with values greater than or equal
     *                 to this value will be considered in the range overlap.
     * @param max      The maximum value of the range. Entities with values less than or equal
     *                 to this value will be considered in the range overlap.
     */
    public RangeDescriptor(FieldDefinition fieldDef, V min, V max) {
        this.fieldDef = fieldDef;
        this.min = min;
        this.max = max;
    }

    /**
     * Constructs a JPA {@link Predicate} representing a range overlap condition for the specified query.
     * The generated predicate ensures that the specified range in the query ([min, max]) overlaps with
     * the range defined in the database record.
     * <p>
     * The overlap logic applied is:
     * - (databaseRangeMin <= queryRangeMax) AND (databaseRangeMax >= queryRangeMin)
     *
     * @param ctx the {@link QueryContext} containing the {@link CriteriaBuilder} and root entity for
     *            constructing the predicate. It is used to resolve paths to database attributes and
     *            create the required comparison expressions.
     * @return a {@link Predicate} representing the range overlap condition, ensuring that the range
     *         defined in the database intersects with the range specified in the query.
     */
    @Override
    public Predicate toPredicate(QueryContext<?> ctx) {
        CriteriaBuilder cb = ctx.criteriaBuilder();

        // 1. Join through planes/energy to get the parent of the 'bounds' object
        From<?, ?> parent = resolveParentPath(ctx.root(), fieldDef);

        // 2. Since 'bounds' is @Embedded, we use .get() twice:
        // once for the embedded object, once for its attribute.
        String lastPart = getLastPart(fieldDef.entityPath());
        Path<V> dbMin = parent.get(lastPart).get(fieldDef.minAttribute());
        Path<V> dbMax = parent.get(lastPart).get(fieldDef.maxAttribute());

        // Overlap Logic: (dbMin <= queryMax) AND (dbMax >= queryMin)
        return cb.and(
                cb.lessThanOrEqualTo(dbMin, max),
                cb.greaterThanOrEqualTo(dbMax, min)
        );
    }

    /**
     * Resolves the parent path in the entity hierarchy based on the provided field definition.
     * This method traverses the entity path specified in the {@code FieldDefinition} sequentially
     * to join the required associations in the path, stopping before reaching the last part of the path.
     *
     * @param root The root {@code From} node representing the starting point of the entity hierarchy.
     * @param def  The {@code FieldDefinition} containing the entity path to be resolved.
     * @return The {@code From} node representing the resolved parent path, just before the last part
     *         of the entity path.
     */
    private From<?, ?> resolveParentPath(From<?, ?> root, FieldDefinition def) {
        String[] parts = def.entityPath().split("\\.");
        From<?, ?> current = root;
        // Join associations (like 'plane' or 'energy')
        // but STOP before the @Embedded 'bounds' object.
        for (int i = 0; i < parts.length - 1; i++) {
            current = current.join(parts[i]);
        }
        return current;
    }

    /**
     * Extracts the last part of a string path that is delimited by dots (".").
     * This method splits the input string into parts based on the delimiter and
     * returns the last segment.
     *
     * @param path the input string representing a path delimited by dots (".")
     * @return the last segment of the path after the final dot, or the entire
     *         string if no delimiter is present
     */
    private String getLastPart(String path) {
        String[] parts = path.split("\\.");
        return parts[parts.length - 1];
    }
}
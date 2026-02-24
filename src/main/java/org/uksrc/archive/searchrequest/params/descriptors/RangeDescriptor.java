package org.uksrc.archive.searchrequest.params.descriptors;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import org.uksrc.archive.searchrequest.params.transform.RangeTransformer;
import org.uksrc.archive.searchrequest.query.QueryContext;

import java.util.ArrayList;
import java.util.List;

import static org.uksrc.archive.searchrequest.params.parser.FieldRegistry.FieldDefinition;

/**
 * Represents a descriptor for defining range-based search criteria in JPA queries. This class
 * builds a predicate that matches entities where a specified range (defined by minimum and maximum values)
 * overlaps with the range defined in the query.
 */
public class RangeDescriptor implements PredicateDescriptor {

    private final FieldDefinition fieldDef;
    private final String min;
    private final String max;

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
    public RangeDescriptor(FieldDefinition fieldDef, String min, String max) {
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
        From<?, ?> parent = resolveParentPath(ctx.root(), fieldDef);

        // Get the logic from the definition
        RangeTransformer rt = fieldDef.transformer();
        Double dbMinVal = rt.transform(min, max, true);
        Double dbMaxVal = rt.transform(min, max, false);

        Path<Double> dbMin = parent.get(fieldDef.minAttribute());
        Path<Double> dbMax = parent.get(fieldDef.maxAttribute());

        List<Predicate> predicates = new ArrayList<>();

        // If we have a transformed max value, apply: dbMin <= userMax
        if (dbMaxVal != null) {
            predicates.add(cb.lessThanOrEqualTo(dbMin, dbMaxVal));
        }

        // If we have a transformed min value, apply: dbMax >= userMin
        if (dbMinVal != null) {
            predicates.add(cb.greaterThanOrEqualTo(dbMax, dbMinVal));
        }

        return cb.and(predicates.toArray(new Predicate[0]));
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

        // Join every single part in the entityPath
        for (String part : parts) {
            current = current.join(part);
        }
        return current;
    }
}
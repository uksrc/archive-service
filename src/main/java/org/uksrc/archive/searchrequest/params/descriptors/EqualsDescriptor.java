package org.uksrc.archive.searchrequest.params.descriptors;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import org.uksrc.archive.searchrequest.query.QueryContext;

import static org.uksrc.archive.searchrequest.params.parser.FieldRegistry.FieldDefinition;

/**
 * The {@code EqualsDescriptor} class represents a descriptor for creating an
 * equality-based predicate in a JPA Criteria API query. It provides a mechanism
 * to dynamically generate conditions where the value of a field in a query
 * matches a specified value.
 *
 * @param <T> The type of the value to be compared for equality.
 */
public class EqualsDescriptor<T> implements PredicateDescriptor {

    private final FieldDefinition fieldDef;
    private final T value;

    /**
     * Constructs a new {@code EqualsDescriptor} instance for specifying an equality condition
     * in a JPA Criteria API query.
     *
     * @param fieldDef  The field definition that specifies the metadata of the entity attribute
     *                  used for range-based comparison. It includes the entity path and optional
     *                  minimum and maximum attribute names.
     * @param value the value to be compared against the field specified by the path
     *              for equality. The type of this value should match the field's type
     *              to ensure compatibility during query execution.
     */
    public EqualsDescriptor(FieldDefinition fieldDef, T value) {
        this.fieldDef = fieldDef;
        this.value = value;
    }

    /**
     * Converts the current {@code EqualsDescriptor} instance into a JPA Criteria API {@link Predicate}
     * to be used in a query. This predicate represents an equality condition between a
     * specified field and a given value.
     *
     * @param context the {@link QueryContext} providing access to the JPA Criteria API utilities,
     *                including the {@link CriteriaBuilder} and the query's root entity.
     * @return a {@link Predicate} that evaluates to {@code true} when the value of the specified field
     *         matches the provided value, and {@code false} otherwise.
     */
    @Override
    public Predicate toPredicate(QueryContext<?> context) {
        Path<?> field = resolvePath(context.root(), fieldDef.entityPath());
        return context.criteriaBuilder().equal(field, value);
    }
}

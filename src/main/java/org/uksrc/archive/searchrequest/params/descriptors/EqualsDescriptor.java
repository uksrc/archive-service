package org.uksrc.archive.searchrequest.params.descriptors;

import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.uksrc.archive.searchrequest.query.QueryContext;

/**
 * The {@code EqualsDescriptor} class represents a descriptor for creating an
 * equality-based predicate in a JPA Criteria API query. It provides a mechanism
 * to dynamically generate conditions where the value of a field in a query
 * matches a specified value.
 *
 * @param <T> The type of the value to be compared for equality.
 */
public class EqualsDescriptor<T> implements PredicateDescriptor {

    private final String path;
    private final T value;

    /**
     * Constructs a new {@code EqualsDescriptor} instance for specifying an equality condition
     * in a JPA Criteria API query.
     *
     * @param path  the dot-separated string representing the path of the field to be used
     *              in the equality condition. This path is typically the field name or
     *              a nested property within an entity.
     * @param value the value to be compared against the field specified by the path
     *              for equality. The type of this value should match the field's type
     *              to ensure compatibility during query execution.
     * @param <T>   the type of the value to be compared for equality.
     */
    public EqualsDescriptor(String path, T value) {
        this.path = path;
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
        Path<?> field = resolvePath(context.root(), path);
        return context.criteriaBuilder().equal(field, value);
    }

    private Path<?> resolvePath(Root<?> root, String path) {
        String[] parts = path.split("\\.");
        Path<?> p = root;
        for (String part : parts) {
            p = p.get(part);
        }
        return p;
    }
}

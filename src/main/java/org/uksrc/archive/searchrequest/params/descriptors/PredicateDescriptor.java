package org.uksrc.archive.searchrequest.params.descriptors;

import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.uksrc.archive.searchrequest.query.QueryContext;

/**
 * A descriptor interface used to define and create criteria-based search conditions in the form of predicates.
 * Implementing classes provide specific logic for generating {@link Predicate} instances tailored to various
 * query scenarios. These predicates are used in conjunction with JPA's Criteria API to filter results based
 * on domain-specific requirements.
 * <p>
 * The {@code PredicateDescriptor} abstracts the process of constructing query predicates by allowing developers
 * to focus on domain-specific query logic. Each implementation defines how a predicate is constructed using
 * the provided {@link QueryContext}.
 */
public interface PredicateDescriptor {
    Predicate toPredicate(QueryContext<?> context);

    /**
     * Resolves the path of an entity by traversing the supplied entity path.
     *
     * @param root The starting point (root) of the entity graph.
     * @param path The entity path to be resolved.
     * @return The resolved path object.
     */
    default Path<?> resolvePath(Root<?> root, String path) {
        String[] parts = path.split("\\.");
        Path<?> p = root;
        for (String part : parts) {
            p = p.get(part);
        }
        return p;
    }
}

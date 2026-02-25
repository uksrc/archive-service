package org.uksrc.archive.searchrequest.params.descriptors;

import jakarta.persistence.criteria.Predicate;
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
}

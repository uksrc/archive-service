package org.uksrc.archive.searchrequest.query;

import jakarta.persistence.criteria.*;

/**
 * Represents a context for building and managing JPA Criteria API queries.
 * This class provides access to the {@link CriteriaBuilder} and the root
 * entity ({@link Root}) of the query, enabling dynamic construction of query predicates.
 *
 * @param <T> The type of the root entity in the JPA query.
 */
public class QueryContext<T> {
    private final CriteriaBuilder cb;
    private final Root<T> root;

    /**
     * Constructs a new instance of {@code QueryContext}, which holds the context
     * for building and managing JPA Criteria API queries. This includes the
     * {@link CriteriaBuilder} for constructing query predicates and the {@link Root}
     * representing the root entity of the query.
     *
     * @param cb the {@link CriteriaBuilder} to be used for constructing query predicates
     * @param root the {@link Root} representing the root entity of the query
     */
    public QueryContext(CriteriaBuilder cb, Root<T> root) {
        this.cb = cb;
        this.root = root;
    }

    public CriteriaBuilder criteriaBuilder() { return cb; }
    public Root<T> root() { return root; }
}

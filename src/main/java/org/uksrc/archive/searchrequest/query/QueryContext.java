package org.uksrc.archive.searchrequest.query;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.ivoa.dm.caom2.Observation;

import java.util.ArrayList;
import java.util.List;

/**
 * A data structure used to encapsulate the context required for building
 * and managing a query using the Search API.
 */
public class QueryContext {
    private final CriteriaBuilder cb;
    private final Root<Observation> root;
    private final List<Predicate> predicates = new ArrayList<>();

    public QueryContext(CriteriaBuilder cb, Root<Observation> root) {
        this.cb = cb;
        this.root = root;
    }

    public CriteriaBuilder criteriaBuilder() { return cb; }
    public Root<Observation> root() { return root; }

    public void add(Predicate predicate) {
        predicates.add(predicate);
    }

    public List<Predicate> predicates() {
        return predicates;
    }
}

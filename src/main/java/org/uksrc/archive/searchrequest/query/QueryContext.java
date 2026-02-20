package org.uksrc.archive.searchrequest.query;

import jakarta.persistence.criteria.*;
import org.ivoa.dm.caom2.Observation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A data structure used to encapsulate the context required for building
 * and managing a query using the Search API.
 */
public class QueryContext {
    private final CriteriaBuilder cb;
    private final Root<Observation> root;
    private final List<Predicate> predicates = new ArrayList<>();

    private final Map<String, Join<?, ?>> joins = new HashMap<>();

    public QueryContext(CriteriaBuilder cb, Root<Observation> root) {
        this.cb = cb;
        this.root = root;
    }

    public CriteriaBuilder criteriaBuilder() { return cb; }
    public Root<Observation> root() { return root; }

    public void add(Predicate predicate) {
        predicates.add(predicate);
    }

  /*  public List<Predicate> predicates() {
        return predicates;
    }*/

    public Path<?> resolvePath(String path) {

        String[] parts = path.split("\\.");

        From<?, ?> current = root;

        for (int i = 0; i < parts.length - 1; i++) {
            current = join(parts[i], current);
        }

        return current.get(parts[parts.length - 1]);
    }

    public From<?, ?> join(String attribute, From<?, ?> from) {
        return joins.computeIfAbsent(attribute,
                a -> from.join(a));
    }

}

package org.uksrc.archive.searchrequest.params.descriptors;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.uksrc.archive.searchrequest.query.QueryContext;

public interface PredicateDescriptor {

    public Predicate toPredicate(QueryContext context);
}

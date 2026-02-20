package org.uksrc.archive.searchrequest.params.descriptors;

import jakarta.persistence.criteria.Predicate;
import org.uksrc.archive.searchrequest.query.QueryContext;

public interface PredicateDescriptor {
    Predicate toPredicate(QueryContext<?> context);
}

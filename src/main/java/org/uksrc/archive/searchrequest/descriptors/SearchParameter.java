package org.uksrc.archive.searchrequest.descriptors;

import org.uksrc.archive.searchrequest.query.QueryContext;
import org.uksrc.archive.searchrequest.schema.ObservationSearchRequest;

/**
 * Represents a search parameter used to filter and refine observation queries
 * in a search operation. The interface provides methods to check whether
 * the parameter is applicable to a given search request and to apply the
 * parameter's conditions to a query context.
 */
public interface SearchParameter {
    void applyIfPresent(ObservationSearchRequest request, QueryContext ctx);
}

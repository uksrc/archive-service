package org.uksrc.archive.searchrequest.descriptors;

import org.uksrc.archive.searchrequest.query.QueryContext;
import org.uksrc.archive.searchrequest.schema.ObservationSearchRequest;

/**
 * The TargetParameter class is an implementation of the SearchParameter interface,
 * designed to filter observation queries based on a specified target.
 * <p>
 * This class checks whether a target value is provided in an ObservationSearchRequest,
 * and if present, it adds a filtering condition to the QueryContext. The filter ensures
 * that query results are constrained to those observations where the target's name matches
 * the requested value.
 * <p>
 * Responsibilities of this class include:
 * - Retrieving the target value from the search request if present.
 * - Constructing a predicate that compares the target name in the data model
 *   to the requested value.
 * - Adding this predicate to the query context, which is later executed as part of the search.
 * <p>
 * This parameter operates specifically within the context of the ObservationSearchRequest
 * and QueryContext classes, enabling dynamic query filtering for observation data.
 */
public class TargetParameter implements SearchParameter {

    @Override
    public void applyIfPresent(ObservationSearchRequest request, QueryContext ctx) {
        request.target().ifPresent(value -> {
            ctx.add(
                    ctx.criteriaBuilder().equal(
                            ctx.root().get("target").get("name"),
                            value
                    )
            );
        });
    }
}

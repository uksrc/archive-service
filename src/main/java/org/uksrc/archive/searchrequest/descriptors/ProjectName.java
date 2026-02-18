package org.uksrc.archive.searchrequest.descriptors;

import org.uksrc.archive.searchrequest.query.QueryContext;
import org.uksrc.archive.searchrequest.schema.ObservationSearchRequest;

/**
 * The ProjectName class is an implementation of the SearchParameter interface,
 * designed to filter observation queries based on a specified project name.
 * <p>
 * This class checks whether a project name is provided in an ObservationSearchRequest,
 * and if present, it adds a filtering condition to the QueryContext. The filter ensures
 * that query results are constrained to those observations where the project's title
 * matches the requested value.
 * <p>
 * Responsibilities of this class include:
 * - Retrieving the project name value from the search request if present.
 * - Constructing a predicate that compares the project's title in the data model
 *   to the supplied value.
 * - Adding this predicate to the query context, which is later executed as part
 *   of the search operation.
 * <p>
 * This parameter operates specifically within the context of the ObservationSearchRequest
 * and QueryContext classes, enabling dynamic query filtering for data involving
 * project titles.
 */
public class ProjectName implements SearchParameter {

    @Override
    public void applyIfPresent(ObservationSearchRequest request, QueryContext ctx) {
        request.projectName().ifPresent(value -> {
            ctx.add(
                    ctx.criteriaBuilder().equal(
                            ctx.root().get("proposal").get("project"),
                            value
                    )
            );
        });
    }
}

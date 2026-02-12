package org.uksrc.archive.searchrequest.descriptors;

import org.uksrc.archive.searchrequest.query.QueryContext;
import org.uksrc.archive.searchrequest.schema.ObservationSearchRequest;

import java.sql.Timestamp;

public class StartDateParameter implements SearchParameter {

    @Override
    public void applyIfPresent(ObservationSearchRequest request, QueryContext ctx) {
        request.startDate().ifPresent(value -> {
            ctx.add(
                    ctx.criteriaBuilder().equal(
                            ctx.root().get("metaRelease"),
                            Timestamp.from(value.toInstant())
                    )
            );
        });
    }
}

package org.uksrc.archive.searchrequest.descriptors;

import org.uksrc.archive.searchrequest.query.QueryContext;
import org.uksrc.archive.searchrequest.schema.ObservationSearchRequest;

public class BandParameter implements SearchParameter {

    @Override
    public void applyIfPresent(ObservationSearchRequest request, QueryContext ctx) {
        request.band().ifPresent(value -> {
            ctx.add(
                    ctx.criteriaBuilder().equal(
                            ctx.root().get("energy").get("band"),
                            value
                    )
            );
        });
    }
}

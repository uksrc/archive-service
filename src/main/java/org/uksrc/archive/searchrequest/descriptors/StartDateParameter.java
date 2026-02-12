package org.uksrc.archive.searchrequest.descriptors;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import org.uksrc.archive.searchrequest.query.QueryContext;
import org.uksrc.archive.searchrequest.schema.ObservationSearchRequest;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;

public class StartDateParameter implements SearchParameter {

    @Override
    public void applyIfPresent(ObservationSearchRequest request, QueryContext ctx) {
        request.startDate().ifPresent(raw -> {
            CriteriaBuilder cb = ctx.criteriaBuilder();
            Path<Timestamp> path = ctx.root().get("metaRelease");

            try {
                // Try full datetime
                OffsetDateTime odt = OffsetDateTime.parse(raw);
                Instant instant = odt.toInstant();

                ctx.add(cb.greaterThanOrEqualTo(path, Timestamp.from(instant)
                ));

            } catch (DateTimeParseException e) {

                // Date-only
                LocalDate date = LocalDate.parse(raw);

                Instant start = date.atStartOfDay(ZoneOffset.UTC).toInstant();
                Instant end = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

                ctx.add(cb.greaterThanOrEqualTo(path, Timestamp.from(start)));
                ctx.add(cb.lessThan(path, Timestamp.from(end)));
            }
        });
    }
}

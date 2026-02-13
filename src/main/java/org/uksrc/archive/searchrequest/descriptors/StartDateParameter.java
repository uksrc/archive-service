package org.uksrc.archive.searchrequest.descriptors;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import org.uksrc.archive.searchrequest.query.QueryContext;
import org.uksrc.archive.searchrequest.schema.ObservationSearchRequest;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;

/**
 * The StartDateParameter class is a specific implementation of the SearchParameter interface
 * that allows filtering observation queries based on a specified start date. This parameter
 * checks if a start date is provided in the observation search request and applies the necessary
 * filters to the query context to include results that satisfy the start date condition.
 *
 * This parameter supports parsing full ISO-8601 formatted date-time strings or simple date strings.
 * When a date-time string is provided, it filters results that occur on or after the specified
 * date-time. If only a date is provided, it filters results occurring on or after the beginning
 * of that day.
 *
 * The main functionality involves:
 * - Parsing the start date value from the search request.
 * - Converting the parsed date to Modified Julian Date (MJD), which is used to represent the date
 *   in astronomical data contexts.
 * - Applying the parsed and formatted date as a filter condition in the query context.
 *
 * The StartDateParameter is specifically used in conjunction with the ObservationSearchRequest
 * and QueryContext classes to refine the list of observations based on the desired time range.
 *
 * Example call: <a href="http://localhost:8080/archive/search/search?startDate=2001-07-01T14:08:52Z">http://localhost:8080/archive/search/search?startDate=2001-07-01T14:08:52Z</a>

 */
public class StartDateParameter implements SearchParameter {

    @Override
    public void applyIfPresent(ObservationSearchRequest request, QueryContext ctx) {

        request.startDate().ifPresent(raw -> {

            CriteriaBuilder cb = ctx.criteriaBuilder();

            Path<Double> lowerPath = ctx.root()
                    .join("planes", JoinType.INNER)
                    .join("time", JoinType.INNER)
                    .join("bounds", JoinType.INNER)
                    .get("lower");

            try {
                // Try full ISO datetime first
                OffsetDateTime odt = OffsetDateTime.parse(raw);
                Instant instant = odt.toInstant();

                double formattedTime = toMjd(instant);
              //  double formattedTime = toEpochSecond(instant);

                //Search for time.bounds.lower values greater than the supplied param value
                ctx.add(cb.greaterThanOrEqualTo(lowerPath, formattedTime));

            } catch (DateTimeParseException e) {
                // Date-only → whole day range
                LocalDate date = LocalDate.parse(raw);
                Instant start = date.atStartOfDay(ZoneOffset.UTC).toInstant();

                double startMjd = toMjd(start);

                ctx.add(cb.greaterThanOrEqualTo(lowerPath, startMjd));
            }
        });
    }

    /**
     * Convert the instant time to Modified Julian Time
     * @param instant an Instant value representing the date to search for.
     * @return double containing the MJD
     */
    private double toMjd(Instant instant) {
        // Use 86400.0 (seconds in a day) and getEpochSecond for cleaner math
        // + 0.5 because MJD starts at midnight, but JD starts at noon
        return (instant.getEpochSecond() / 86400.0) + (instant.getNano() / 86400000000000.0) + 40587.0;
    }

    private double toEpochSecond(Instant instant) {
        return instant.getEpochSecond();
    }

}

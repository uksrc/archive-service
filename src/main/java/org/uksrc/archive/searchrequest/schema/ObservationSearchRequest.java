package org.uksrc.archive.searchrequest.schema;

import jakarta.ws.rs.QueryParam;

import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * Represents a request to search for observations based on specific criteria.
 * This class is used to encapsulate query parameters for filtering observations.
 */
public class ObservationSearchRequest {
    @QueryParam("target")
    public String target;

    @QueryParam("band")
    public String band;

    @QueryParam("startDate")
    public OffsetDateTime startDate;

    // Helper methods (cleaner than null checks everywhere)
    public Optional<String> target() {
        return Optional.ofNullable(target);
    }

    public Optional<String> band() { return Optional.ofNullable(band); }

    public Optional<OffsetDateTime> startDate() {
        return Optional.ofNullable(startDate);
    }
}

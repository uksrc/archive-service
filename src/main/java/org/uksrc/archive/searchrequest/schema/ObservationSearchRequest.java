package org.uksrc.archive.searchrequest.schema;

import jakarta.ws.rs.QueryParam;

import java.util.Optional;

/**
 * Represents a request to search for observations based on specific criteria.
 * This class is used to encapsulate query parameters for filtering observations.
 */
public class  ObservationSearchRequest {
    @QueryParam("target")
    public String target;

    @QueryParam("band")
    public String band;

    @QueryParam("startDate")
    public String startDate;

    @QueryParam("projectName")
    public String projectName;

    // Helper methods (cleaner than null checks everywhere)
    public Optional<String> target() {
        return Optional.ofNullable(target);
    }

    public Optional<String> band() { return Optional.ofNullable(band); }

    public Optional<String> startDate() {
        return Optional.ofNullable(startDate);
    }

    public Optional<String> projectName() { return Optional.ofNullable(projectName); }
}

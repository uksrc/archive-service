package org.uksrc.archive.searchrequest.params.transform;

import java.time.*;
import java.time.format.DateTimeParseException;


public class Transformers {

    private static final Instant MJD_EPOCH =
            LocalDate.of(1858, 11, 17)
                    .atStartOfDay(ZoneOffset.UTC)
                    .toInstant();

    // Standard numeric range: Simple parsing
    public static final RangeTransformer BASIC_NUMBER = (min, max, lower) -> {
        String val = lower ? min : max;
        return (val != null) ? Double.parseDouble(val) : null;
    };

    // Spectral: String -> Double -> Math -> Flip
    public static final RangeTransformer FREQUENCY_TO_WAVELENGTH = (min, max, lower) -> {
        double c = 299792458.0;
        Double fMin = (min != null) ? Double.parseDouble(min) : null;
        Double fMax = (max != null) ? Double.parseDouble(max) : null;

        if (lower) {
            return (fMax != null && fMax != 0) ? c / fMax : null;
        } else {
            return (fMin != null && fMin != 0) ? c / fMin : null;
        }
    };

    // Time: String -> ISO8601 -> MJD Seconds
    public static final RangeTransformer ISO_TO_MJD = (min, max, lower) -> {
        String val = lower ? min : max;
        if (val == null) return null;

        try {
            // Try full ISO datetime first
            OffsetDateTime odt = OffsetDateTime.parse(min);
            Instant instant = odt.toInstant();

            return toMjdSeconds(instant);
        } catch (DateTimeParseException e) {
            // Date-only → whole day range
            LocalDate date = LocalDate.parse(min);
            Instant start = date.atStartOfDay(ZoneOffset.UTC).toInstant();

            return toMjdSeconds(start);
        }
    };

    public static final RangeTransformer STRICT_START_TIME = (min, max, lower) -> {
        // If we are calculating the value to compare against dbMin (lower)
        if (lower) {
            OffsetDateTime odt = OffsetDateTime.parse(min);
            Instant instant = odt.toInstant();

            return toMjdSeconds(instant);
        } else {
            // If we return null for the upper bound calculation,
            // the Predicate logic needs to skip the 'lessThan' check.
            return null;
        }
    };

    /**
     * Convert the Instant to MJD Seconds (Seconds since 1858-11-17T00:00:00Z)
     * @param instant an Instant value representing the date to search for.
     * @return double containing the MJD Seconds
     */
     private static double toMjdSeconds(Instant instant) {
         return Duration.between(MJD_EPOCH, instant).toNanos() / 1_000_000_000.0;
    }
}

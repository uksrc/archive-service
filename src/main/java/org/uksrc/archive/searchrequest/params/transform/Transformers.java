package org.uksrc.archive.searchrequest.params.transform;

import java.time.*;
import java.time.format.DateTimeParseException;

/**
 * The Transformers class provides a collection of predefined {@code RangeTransformer}
 * implementations to facilitate bound conversions for various data types including
 * numeric ranges, spectral conversions, and temporal transformations.
 */
public class Transformers {

    //Used for conversion to Modified Julian Date (CAOM uses MJD 'seconds')
    private static final Instant MJD_EPOCH =
            LocalDate.of(1858, 11, 17)
                    .atStartOfDay(ZoneOffset.UTC)
                    .toInstant();

    /**
     * A predefined {@code RangeTransformer} implementation that converts frequency (in Hz) to
     * wavelength (in metres) using the formula {@code λ = c / f}, where {@code c} is the speed
     * of light in vacuum (299,792,458 m/s). This transformer determines the lower or upper bound
     * of the wavelength range based on the {@code lower} parameter.
     *
     * <ul>
     *   <li>If {@code lower} is {@code true}, the transformer calculates the wavelength corresponding
     *       to the maximum frequency ({@code max}).</li>
     *   <li>If {@code lower} is {@code false}, the transformer calculates the wavelength corresponding
     *       to the minimum frequency ({@code min}).</li>
     * </ul>
     *
     * <p>Behavior:
     * <ul>
     *   <li>If the {@code max} or {@code min} frequency is {@code null}, the transformer
     *       returns {@code null}.</li>
     *   <li>If the selected frequency is zero, the transformer returns {@code null} to
     *       avoid a division by zero.</li>
     *   <li>The inputs {@code min} and {@code max} are expected to be parsable strings representing
     *       valid non-negative values, otherwise a {@code NumberFormatException} will be thrown.</li>
     * </ul>
     *
     * @see RangeTransformer
     */
    public static final RangeTransformer FREQUENCY_TO_WAVELENGTH = (min, max, lower) -> {
        double c = 299792458.0;
        Double fMin = (min != null) ? Double.parseDouble(min) : null;
        Double fMax = (max != null) ? Double.parseDouble(max) : null;

        //Account for the value flip between frequency and wavelength.
        if (lower) {
            return (fMax != null && fMax != 0) ? c / fMax : null;
        } else {
            return (fMin != null && fMin != 0) ? c / fMin : null;
        }
    };

    /**
     * A predefined {@code RangeTransformer} implementation that converts ISO-8601 formatted
     * date-time strings into Modified Julian Date (MJD) seconds for temporal range calculations.
     * The transformer determines the lower or upper bound based on the {@code lower} parameter and
     * processes the input accordingly.
     *
     * <ul>
     *   <li>If {@code lower} is {@code true}, the transformer uses the {@code min} value as the input.</li>
     *   <li>If {@code lower} is {@code false}, the transformer uses the {@code max} value as the input.</li>
     * </ul>
     *
     * The input is then parsed as either:
     * <ul>
     *   <li>A full ISO-8601 date-time string, which is directly converted to an {@code Instant}, or</li>
     *   <li>A date-only ISO-8601 string, where the start of the day in UTC is used for the calculation.</li>
     * </ul>
     *
     * If the input cannot be parsed as ISO-8601, this transformer will throw a {@code DateTimeParseException}.
     * The resulting {@code Instant} is converted to seconds since the Modified Julian Date epoch
     * ('1858-11-17T00:00:00Z').
     *
     * @see RangeTransformer
     * @see Transformers#toMjdSeconds(Instant)
     */
    public static final RangeTransformer ISO_TO_MJD = (min, max, lower) -> {
        String val = lower ? min : max;
        if (val == null) return null;

        try {
            // Try full ISO datetime first
            OffsetDateTime odt = OffsetDateTime.parse(min);
            Instant instant = odt.toInstant();

            return toMjdSeconds(instant);
        } catch (DateTimeParseException e) {
            // Date-only
            LocalDate date = LocalDate.parse(min);
            Instant start = date.atStartOfDay(ZoneOffset.UTC).toInstant();

            return toMjdSeconds(start);
        }
    };

    /**
     * A predefined {@code RangeTransformer} implementation that enforces strict lower bound
     * calculations for temporal data. This transformer converts an ISO-8601 formatted date-time
     * input into Modified Julian Date (MJD) seconds for the lower bound, and skips the upper
     * bound calculation by returning {@code null}.
     *
     * <ul>
     *   <li>If {@code lower} is {@code true}, this transformer parses the {@code min} string
     *       as an ISO-8601 date-time, converts it into an {@code Instant}, and calculates its
     *       MJD seconds representation.</li>
     *   <li>If {@code lower} is {@code false}, the transformer returns {@code null}, meaning
     *       the upper bound should not be applied in range calculations.</li>
     * </ul>
     *
     * This behaviour ensures that it provides a strict boundary for temporal lower bound
     * queries while allowing conditions to bypass the upper bound filtering logic.
     * Allowing the {@code RangeDescriptor} to be used to start/lower bounds only (typically '>=')
     * <p>
     * Note: The MJD seconds representation is calculated relative to the epoch
     * '1858-11-17T00:00:00Z'.
     *
     * @see Transformers#ISO_TO_MJD
     * @see Transformers#toMjdSeconds(Instant)
     */
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

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
     * @see Transformer
     */
    public static final Transformer FREQUENCY_TO_WAVELENGTH = new Transformer() {
        final double c = 299792458.0;

        @Override
        public Double transform(String value) {
            return c / Double.parseDouble(value);
        }

        @Override
        public Double transform(String min, String max, boolean useLower) {
            Double fMin = (min != null) ? Double.parseDouble(min) : null;
            Double fMax = (max != null) ? Double.parseDouble(max) : null;

            //Account for the value flip between frequency and wavelength.
            if (useLower) {
                return (fMax != null && fMax != 0) ? c / fMax : null;
            } else {
                return (fMin != null && fMin != 0) ? c / fMin : null;
            }
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
     * @see Transformer
     * @see Transformers#toMjdSeconds(Instant)
     */
    public static final Transformer ISO_TO_MJD = new Transformer() {

        @Override
        public Double transform(String value) {
            return convertTime(value);
        }

        @Override
        public Double transform(String min, String max, boolean useLower) {
            if (useLower) {
                return convertTime(min);
            } else {
                return convertTime(max);
            }
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

    /**
     * Converts a string representation of a date or datetime into Modified Julian Date (MJD) seconds.
     * The method attempts to parse the input string as a full ISO-8601 datetime first;
     * if the parsing fails, it falls back to interpreting the string as a date-only value.
     *
     * @param value the input string to be converted, representing a datetime or date in ISO-8601 format
     * @return the corresponding value in Modified Julian Date (MJD) seconds as a double
     * @throws DateTimeParseException if the input string cannot be parsed as a valid ISO-8601 datetime or date
     */
    private static Double convertTime(String value) {
        if (value != null) {
            try {
                // Try full ISO datetime first
                OffsetDateTime odt = OffsetDateTime.parse(value);
                Instant instant = odt.toInstant();

                return toMjdSeconds(instant);
            } catch (DateTimeParseException e) {
                // Date-only
                LocalDate date = LocalDate.parse(value);
                Instant start = date.atStartOfDay(ZoneOffset.UTC).toInstant();

                return toMjdSeconds(start);
            }
        }
        return null;
    }
}

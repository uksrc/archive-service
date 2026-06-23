package org.uksrc.archive.searchrequest.params.transform;

/**
 * The {@code RangeTransformer} functional interface represents a contract for transforming
 * a range defined by a minimum and maximum value into a specific bound. This transformation
 * is conditionally determined based on whether the lower or upper bound needs to be derived.
 * <p>
 * The typical use case for this interface involves data range processing, such as converting
 * numeric, temporal, or spectral values into database-specific boundary values. Implementations
 * of this interface allow for flexible transformations based on the provided input range and
 * the specified bound type.
 * <p>
 * Functional Specification:
 * - The method {@code transform} accepts three parameters:
 *   1. A {@code min} value as a {@code String}, representing the lower boundary of the range.
 *   2. A {@code max} value as a {@code String}, representing the upper boundary of the range.
 *   3. A {@code boolean} flag {@code isLowerBound} that determines whether the lower or upper
 *      bound of the range is to be calculated.
 * <p>
 * Behavioral Notes:
 * - This interface is intended to be used in a variety of transformation contexts, such as
 *   converting frequencies to wavelengths, ISO-8601 date strings to Modified Julian Date (MJD) seconds, etc.
 * - Implementations may throw runtime exceptions such as {@code NumberFormatException} or
 *   {@code DateTimeParseException} if the input strings cannot be parsed correctly.
 * - When either {@code min} or {@code max} is {@code null}, the implementation may return
 *   {@code null}, reflecting the absence of a calculated bound.
 * <p>
 * Example implementations can include:
 * - Transformations to Modified Julian Date for temporal data.
 * - Frequency-to-wavelength transformations for spectral data.
 * - Custom range-specific transformations.
 */
public interface Transformer {

    /**
     * Transforms the provided string value into a numeric representation as a Double.
     * The specific transformation behaviour is determined by the implementation of this method.
     *
     * @param value the input string value to be transformed; may represent a numeric or other domain-specific value
     * @return the transformed value as a Double, or null if the transformation fails or the input is invalid
     */
    Double transform(String value);

    /**
     * Transforms the range specified by the minimum and maximum values into a specific bound.
     * The transformation behaviour is influenced by the useLower parameter, which determines
     * whether the resulting value corresponds to the lower or upper bound of the range.
     * Required due to some transforms requiring both bounds (such as frequency-to-wavelength flip).
     *
     * @param min the minimum value of the range as a String; represents the lower boundary of the range
     * @param max the maximum value of the range as a String; represents the upper boundary of the range
     * @param useLower a boolean flag indicating whether to calculate the lower bound (true)
     *                 or the upper bound (false) of the range
     * @return the calculated boundary value as a Double, or null if the inputs are invalid or the transformation cannot be performed
     */
    Double transform(String min, String max, boolean useLower);
}
package com.interviewprep.config;

/**
 * Utility class for parsing string values to Long with null and blank handling.
 * 
 * <p>This utility provides a safe way to parse strings to Long values, returning {@code null}
 * instead of throwing exceptions when the input is null, blank, or contains invalid numeric data.</p>
 * 
 * <p>Usage example:</p>
 * <pre>{@code
 * String idStr = "123";
 * Long parsedId = ParsingUtils.parseLongSafe(idStr);
 * if (parsedId != null) {
 *     // Use the ID...
 * }
 * }</pre>
 */
public final class ParsingUtils {

    private ParsingUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Parses a string to a {@link Long} value, returning {@code null} if the input is invalid.
     * 
     * <p>This method handles the following cases safely:</p>
     * <ul>
     *   <li>{@code null} input → returns {@code null}</li>
     *   <li>Blank or whitespace-only string → returns {@code null}</li>
     *   <li>Non-numeric string (e.g., "abc", "12.5") → returns {@code null}</li>
     * </ul>
     * 
     * @param value the string to parse, may be {@code null} or blank
     * @return the parsed Long value, or {@code null} if parsing fails
     */
    public static Long parseLongSafe(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            // Return null for invalid numeric strings instead of throwing exception
            return null;
        }
    }

    /**
     * Parses a string to an {@link Integer} value, returning {@code null} if the input is invalid.
     * 
     * <p>This method handles the following cases safely:</p>
     * <ul>
     *   <li>{@code null} input → returns {@code null}</li>
     *   <li>Blank or whitespace-only string → returns {@code null}</li>
     *   <li>Non-numeric string (e.g., "abc", "12.5") → returns {@code null}</li>
     * </ul>
     * 
     * @param value the string to parse, may be {@code null} or blank
     * @return the parsed Integer value, or {@code null} if parsing fails
     */
    public static Integer parseIntSafe(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            // Return null for invalid numeric strings instead of throwing exception
            return null;
        }
    }

    /**
     * Checks if a string represents a valid non-null Long value.
     * 
     * @param value the string to check, may be {@code null} or blank
     * @return {@code true} if the string can be parsed as a Long, {@code false} otherwise
     */
    public static boolean isValidLong(String value) {
        return parseLongSafe(value) != null;
    }

    /**
     * Checks if a string represents a valid non-null Integer value.
     * 
     * @param value the string to check, may be {@code null} or blank
     * @return {@code true} if the string can be parsed as an Integer, {@code false} otherwise
     */
    public static boolean isValidInteger(String value) {
        return parseIntSafe(value) != null;
    }

    /**
     * Parses a string to a Long, or returns the default value if parsing fails.
     * 
     * @param value the string to parse, may be {@code null} or blank
     * @param defaultValue the value to return if parsing fails
     * @return the parsed Long value, or {@code defaultValue} if parsing fails
     */
    public static Long parseLongSafe(String value, Long defaultValue) {
        Long result = parseLongSafe(value);
        return result != null ? result : defaultValue;
    }

    /**
     * Parses a string to an Integer, or returns the default value if parsing fails.
     * 
     * @param value the string to parse, may be {@code null} or blank
     * @param defaultValue the value to return if parsing fails
     * @return the parsed Integer value, or {@code defaultValue} if parsing fails
     */
    public static Integer parseIntSafe(String value, Integer defaultValue) {
        Integer result = parseIntSafe(value);
        return result != null ? result : defaultValue;
    }
}

package com.interviewprep.config;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Data Transfer Object representing a single validation error field.
 * 
 * <p>Used by {@link ValidationExceptionHandler} to format constraint violations into a consistent JSON response structure.</p>
 */
public record ValidationError(
        String field,
        String message,
        String rejectedValue
) {

    /**
     * Creates a ValidationError from Jakarta Bean Validation's FieldError.
     * 
     * @param fieldName the validated field name
     * @param message the constraint violation message
     * @param rejectedValue the invalid value that triggered the violation (may be null)
     */
    public static ValidationError of(String fieldName, String message, Object rejectedValue) {
        return new ValidationError(
                fieldName != null ? fieldName : "(unknown)",
                message,
                rejectedValue != null ? rejectedValue.toString() : null
        );
    }

    /**
     * Creates a generic (non-field-specific) validation error.
     * 
     * @param message the violation message
     */
    public static ValidationError generic(String message) {
        return new ValidationError("(validation)", message, null);
    }
}

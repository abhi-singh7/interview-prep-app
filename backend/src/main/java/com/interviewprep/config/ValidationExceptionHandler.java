package com.interviewprep.config;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Global exception handler for Jakarta Bean Validation errors.
 * 
 * <p>Catches validation exceptions thrown by {@code @Valid} annotated request body parameters and
 * formats them into a consistent JSON response structure with field name, violation message,
 * and rejected value.</p>
 */
@ControllerAdvice
public class ValidationExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(ValidationExceptionHandler.class);

    /**
     * Handles {@link MethodArgumentNotValidException} thrown when a request body fails Bean Validation.
     * 
     * <p>This is triggered by Spring MVC when {@code @Valid} is used on a {@code @RequestBody} parameter
     * and one or more constraints (e.g., {@code @NotBlank}, {@code @Size}) are violated during deserialization.</p>
     * 
     * @param ex the method argument validation exception
     * @return ResponseEntity with 400 Bad Request status and a body containing field-level validation errors
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        List<ValidationError> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> ValidationError.of(
                        fieldError.getField(),
                        fieldError.getDefaultMessage(),
                        fieldError.getRejectedValue()))
                .toList();

        logger.info("Validation errors: {}", errors);

        Map<String, Object> body = Map.of(
                "status", HttpStatus.BAD_REQUEST.value(),
                "timestamp", LocalDateTime.now().toString(),
                "errors", errors.stream()
                        .map(e -> {
                            java.util.HashMap<String, Object> map = new java.util.HashMap<>();
                            map.put("field", e.field());
                            map.put("message", e.message());
                            if (e.rejectedValue() != null) {
                                map.put("rejectedValue", e.rejectedValue());
                            }
                            return map;
                        })
                        .toList());

        return ResponseEntity.badRequest().body(body);
    }

    /**
     * Handles {@link ConstraintViolationException} thrown when cross-field or method-level constraints are violated.
     * 
     * <p>This catches violations that occur outside of request body deserialization, such as programmatic constraint checks.</p>
     * 
     * @param ex the constraint violation exception
     * @return ResponseEntity with 400 Bad Request status and a body containing constraint violation details
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolationExceptions(ConstraintViolationException ex) {
        List<ValidationError> errors = ex.getConstraintViolations().stream()
                .map((java.util.function.Function<ConstraintViolation<?>, ValidationError>) violation ->
                        ValidationError.of(violation.getPropertyPath().toString(), violation.getMessage(), null))
                .toList();

        logger.info("Constraint violations: {}", errors);

        Map<String, Object> body = Map.of(
                "status", HttpStatus.BAD_REQUEST.value(),
                "timestamp", LocalDateTime.now().toString(),
                "errors", errors.stream()
                        .map(e -> Map.<String, Object>of(
                                "field", e.field(),
                                "message", e.message()))
                        .toList());

        return ResponseEntity.badRequest().body(body);
    }
}

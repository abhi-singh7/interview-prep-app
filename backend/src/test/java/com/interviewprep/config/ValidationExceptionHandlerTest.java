package com.interviewprep.config;

import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ValidationExceptionHandlerTest {

    private final ValidationExceptionHandler handler = new ValidationExceptionHandler();

    @Test
    void handles_method_argument_not_valid_exception() {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(
            new Object(), "registerRequest");
        bindingResult.addError(new org.springframework.validation.FieldError(
            "registerRequest", "email", "must be a valid email"));
        
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

        ResponseEntity<Map<String, Object>> response = handler.handleValidationExceptions(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        Map<String, Object> body = response.getBody();
        assertThat(body).containsKey("errors");
    }

    @Test
    void handles_constraint_violation_exception() {
        ConstraintViolationException ex = new ConstraintViolationException(
            "test violation", Set.of());

        ResponseEntity<Map<String, Object>> response = handler.handleConstraintViolationExceptions(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsKey("errors");
    }

    @Test
    void validation_error_record_creation() {
        ValidationError err = ValidationError.of("password", "min length 6", "abc");
        assertThat(err.field()).isEqualTo("password");
        assertThat(err.message()).isEqualTo("min length 6");
        assertThat(err.rejectedValue()).isEqualTo("abc");

        ValidationError generic = ValidationError.generic("invalid input");
        assertThat(generic.field()).isEqualTo("(validation)");
        assertThat(generic.rejectedValue()).isNull();
    }
}

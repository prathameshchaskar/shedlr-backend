package com.shedlr.authservice.common.exception;

import com.shedlr.authservice.identity.dto.response.GenericMessageResponse;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * GlobalExceptionHandler catches exceptions thrown across the application and converts them
 * into stable, consistent API error responses. This prevents leaking internal stack traces
 * and ensures a better developer experience for frontend consumers.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles validation errors from @Valid annotations on DTOs.
     * Returns a map of field names and their corresponding error messages.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
    }

    /**
     * Handles authentication failures (incorrect email/password).
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<GenericMessageResponse> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new GenericMessageResponse("Invalid email or password"));
    }

    /**
     * Handles common illegal state exceptions (e.g., email already registered).
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<GenericMessageResponse> handleIllegalState(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new GenericMessageResponse(ex.getMessage()));
    }

    /**
     * Handles common illegal argument exceptions (e.g., passwords don't match).
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<GenericMessageResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new GenericMessageResponse(ex.getMessage()));
    }

    /**
     * Catch-all handler for any other unexpected exceptions.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<GenericMessageResponse> handleGeneralException(Exception ex) {
        // Log the actual exception for internal debugging
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new GenericMessageResponse("An unexpected error occurred"));
    }
}

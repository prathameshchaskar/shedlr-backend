package com.shedlr.authservice.common.exception;

import com.shedlr.authservice.common.exception.base.AuthException;
import com.shedlr.authservice.common.exception.base.ShedlrException;
import com.shedlr.authservice.common.exception.dto.ApiErrorResponse;
import com.shedlr.authservice.common.exception.errorcode.ErrorCode;
import com.shedlr.authservice.identity.dto.response.GenericMessageResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * GlobalExceptionHandler catches exceptions thrown across the application and converts them
 * into stable, consistent API error responses. This prevents leaking internal stack traces
 * and ensures a better developer experience for frontend consumers.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Handles custom ShedlrException hierarchy.
     */
    @ExceptionHandler(ShedlrException.class)
    public ResponseEntity<ApiErrorResponse> handleShedlrException(ShedlrException ex, HttpServletRequest request) {
        ErrorCode errorCode = ex.getErrorCode();
        ApiErrorResponse response = new ApiErrorResponse(
                errorCode.getStatus().value(),
                errorCode.name(),
                ex.getMessage(),
                request.getRequestURI(),
                OffsetDateTime.now()
        );
        return ResponseEntity.status(errorCode.getStatus()).body(response);
    }

    /**
     * Handles validation errors from @Valid annotations on DTOs.
     * Returns a map of field names and their corresponding error messages.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        ApiErrorResponse response = new ApiErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                ErrorCode.VALIDATION_ERROR.name(),
                "Input validation failed: " + errors,
                request.getRequestURI(),
                OffsetDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handles authentication failures (incorrect email/password).
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleBadCredentials(BadCredentialsException ex, HttpServletRequest request) {
        ApiErrorResponse response = new ApiErrorResponse(
                HttpStatus.UNAUTHORIZED.value(),
                ErrorCode.AUTH_BAD_CREDENTIALS.name(),
                ErrorCode.AUTH_BAD_CREDENTIALS.getMessage(),
                request.getRequestURI(),
                OffsetDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    /**
     * Handles locked account attempts.
     */
    @ExceptionHandler(org.springframework.security.authentication.LockedException.class)
    public ResponseEntity<ApiErrorResponse> handleLockedAccount(org.springframework.security.authentication.LockedException ex, HttpServletRequest request) {
        ApiErrorResponse response = new ApiErrorResponse(
                HttpStatus.LOCKED.value(),
                ErrorCode.AUTH_ACCOUNT_LOCKED.name(),
                ErrorCode.AUTH_ACCOUNT_LOCKED.getMessage(),
                request.getRequestURI(),
                OffsetDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.LOCKED).body(response);
    }

    /**
     * Catch-all handler for any other unexpected exceptions.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneralException(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error occurred: ", ex);
        ApiErrorResponse response = new ApiErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                ErrorCode.GENERIC_ERROR.name(),
                "An unexpected error occurred: " + ex.getMessage(),
                request.getRequestURI(),
                OffsetDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}

package com.shedlr.authservice.common.exception.errorcode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * ErrorCode registry for the entire application.
 * Each code is associated with a specific HTTP status.
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    // General
    GENERIC_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred"),
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "Validation failed"),
    RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "Too many requests. Please try again later."),

    // Signup
    SIGNUP_EMAIL_EXISTS(HttpStatus.CONFLICT, "Email already registered"),
    SIGNUP_PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "Passwords do not match"),
    SIGNUP_INVALID_DATA(HttpStatus.BAD_REQUEST, "Invalid signup data"),

    // Verification
    VERIFY_TOKEN_INVALID(HttpStatus.BAD_REQUEST, "Invalid verification token"),
    VERIFY_TOKEN_EXPIRED(HttpStatus.GONE, "Verification link has expired"),
    VERIFY_ALREADY_DONE(HttpStatus.OK, "Email already verified"),
    VERIFY_TOKEN_REUSED(HttpStatus.BAD_REQUEST, "Verification link has already been used"),

    // Authentication
    AUTH_BAD_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Invalid email or password"),
    AUTH_NOT_VERIFIED(HttpStatus.FORBIDDEN, "Email is not verified"),
    AUTH_ACCOUNT_LOCKED(HttpStatus.LOCKED, "Account is temporarily locked due to many failed attempts"),
    AUTH_ACCOUNT_DISABLED(HttpStatus.FORBIDDEN, "Account has been disabled"),
    AUTH_ACCOUNT_SUSPENDED(HttpStatus.FORBIDDEN, "Account has been suspended"),
    AUTH_ACCOUNT_EXPIRED(HttpStatus.FORBIDDEN, "Account has expired"),
    AUTH_CREDENTIALS_EXPIRED(HttpStatus.FORBIDDEN, "Credentials have expired"),

    // Session / JWT
    SESSION_EXPIRED(HttpStatus.UNAUTHORIZED, "Session has expired"),
    SESSION_INVALID(HttpStatus.UNAUTHORIZED, "Invalid session token"),
    REFRESH_TOKEN_INVALID(HttpStatus.BAD_REQUEST, "Invalid or expired refresh token"),
    REFRESH_TOKEN_EXPIRED(HttpStatus.BAD_REQUEST, "Refresh token has expired"),
    REFRESH_TOKEN_REUSED(HttpStatus.UNAUTHORIZED, "Security alert: Refresh token already used"),

    // Password Reset
    RESET_TOKEN_INVALID(HttpStatus.BAD_REQUEST, "Invalid or expired reset token"),
    RESET_TOKEN_EXPIRED(HttpStatus.GONE, "Reset link has expired"),
    RESET_PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "Passwords do not match"),

    // Audit / Security
    AUDIT_LOG_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to record audit log");

    private final HttpStatus status;
    private final String message;
}

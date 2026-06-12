package com.shedlr.authservice.identity.dto.request;

import jakarta.validation.constraints.*;

/**
 * ResendVerificationRequest is used to request a new email verification token.
 */
public record ResendVerificationRequest(
    /** The email address associated with the account. */
    @NotBlank(message = "Email is required") 
    @Email(message = "Invalid email format") 
    @Size(max = 320) 
    String email
) {}

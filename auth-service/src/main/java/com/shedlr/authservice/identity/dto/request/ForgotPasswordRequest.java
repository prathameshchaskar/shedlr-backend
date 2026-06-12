package com.shedlr.authservice.identity.dto.request;

import jakarta.validation.constraints.*;

/**
 * ForgotPasswordRequest is used to initiate the password recovery process.
 */
public record ForgotPasswordRequest(
    /** The email address to which the reset link should be sent. */
    @NotBlank(message = "Email is required") 
    @Email(message = "Invalid email format") 
    @Size(max = 320) 
    String email
) {}

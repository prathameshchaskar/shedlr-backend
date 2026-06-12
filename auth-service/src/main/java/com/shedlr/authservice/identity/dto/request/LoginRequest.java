package com.shedlr.authservice.identity.dto.request;

import jakarta.validation.constraints.*;

/**
 * LoginRequest contains credentials for user authentication.
 */
public record LoginRequest(
    /** Registered email address. */
    @NotBlank(message = "Email is required") 
    @Email(message = "Invalid email format") 
    @Size(max = 320) 
    String email,

    /** Plaintext password to be verified against the stored hash. */
    @NotBlank(message = "Password is required") 
    @Size(max = 100) 
    String password,

    /** Optional flag to request a longer session duration. */
    Boolean rememberMe
) {}

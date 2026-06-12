package com.shedlr.authservice.identity.dto.request;

import jakarta.validation.constraints.*;

/**
 * SignupRequest encapsulates the data required for a new user to register.
 * Uses Bean Validation to enforce data integrity at the entry point.
 */
public record SignupRequest(
    /** The user's full name. */
    @NotBlank(message = "Full name is required") 
    @Size(max = 150) 
    String fullName,

    /** The email address to register. Used as the unique identifier. */
    @NotBlank(message = "Email is required") 
    @Email(message = "Invalid email format") 
    @Size(max = 320) 
    String email,

    /** The desired password. Recommended minimum 8 characters. */
    @NotBlank(message = "Password is required") 
    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters") 
    String password,

    /** Password confirmation to ensure the user typed it correctly. */
    @NotBlank(message = "Confirm password is required") 
    @Size(min = 8, max = 100) 
    String confirmPassword,

    /** Mandatory acceptance of terms and conditions. */
    @NotNull(message = "You must accept the terms and conditions")
    @AssertTrue(message = "You must accept the terms and conditions") 
    Boolean acceptTerms
) {}

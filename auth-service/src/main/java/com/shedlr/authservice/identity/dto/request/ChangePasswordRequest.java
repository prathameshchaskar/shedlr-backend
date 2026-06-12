package com.shedlr.authservice.identity.dto.request;

import jakarta.validation.constraints.*;

/**
 * ChangePasswordRequest is used by authenticated users to update their password.
 */
public record ChangePasswordRequest(
    /** The user's current password for verification. */
    @NotBlank(message = "Current password is required") 
    String currentPassword,

    /** The new password to be set. */
    @NotBlank(message = "New password is required") 
    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters") 
    String newPassword,

    /** Confirmation of the new password. */
    @NotBlank(message = "Confirm password is required") 
    @Size(min = 8, max = 100) 
    String confirmPassword
) {}

package com.shedlr.authservice.identity.dto.request;

import jakarta.validation.constraints.*;

/**
 * ResetPasswordRequest is used to set a new password after verifying a reset token.
 */
public record ResetPasswordRequest(
    /** The verification token received via email. */
    @NotBlank(message = "Token is required") 
    String token,

    /** The new password to be set. */
    @NotBlank(message = "New password is required") 
    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters") 
    String newPassword,

    /** Confirmation of the new password. */
    @NotBlank(message = "Confirm password is required") 
    @Size(min = 8, max = 100) 
    String confirmPassword
) {}

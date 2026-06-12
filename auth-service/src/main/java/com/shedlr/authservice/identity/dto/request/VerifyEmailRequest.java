package com.shedlr.authservice.identity.dto.request;

import jakarta.validation.constraints.*;

/**
 * VerifyEmailRequest is used to confirm a user's email address using a token.
 */
public record VerifyEmailRequest(
    /** The verification token sent to the user's email. */
    @NotBlank(message = "Token is required") 
    String token
) {}

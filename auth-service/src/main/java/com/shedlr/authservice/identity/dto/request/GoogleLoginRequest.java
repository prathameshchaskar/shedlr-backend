package com.shedlr.authservice.identity.dto.request;

import jakarta.validation.constraints.*;

/**
 * GoogleLoginRequest carries the ID token obtained from Google.
 */
public record GoogleLoginRequest(
    /** The OAuth2 ID token from Google to be verified by the backend. */
    @NotBlank(message = "ID Token is required") 
    String idToken
) {}

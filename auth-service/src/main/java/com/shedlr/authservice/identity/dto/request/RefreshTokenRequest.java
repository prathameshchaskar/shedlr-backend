package com.shedlr.authservice.identity.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * RefreshTokenRequest is used to request a new access token using a refresh token.
 */
public record RefreshTokenRequest(
    @NotBlank(message = "Refresh token is required")
    String refreshToken
) {}

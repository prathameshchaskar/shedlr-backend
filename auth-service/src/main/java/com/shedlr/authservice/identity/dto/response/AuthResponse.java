package com.shedlr.authservice.identity.dto.response;

/**
 * AuthResponse is returned upon successful login or token refresh.
 */
public record AuthResponse(
    /** JWT access token for authenticating subsequent requests. */
    String accessToken,

    /** Refresh token used to obtain a new access token when it expires. */
    String refreshToken,

    /** Type of token, typically 'Bearer'. */
    String tokenType,

    /** Validity period of the access token in seconds. */
    long expiresInSeconds,

    /** Summary information about the authenticated user. */
    UserSummaryResponse user
) {}

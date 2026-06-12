package com.shedlr.authservice.identity.dto.response;

/**
 * GenericMessageResponse is a standard wrapper for simple text responses.
 * Used for actions like "verification email sent" or "password updated".
 */
public record GenericMessageResponse(
    String message
) {}

package com.shedlr.authservice.common.exception.dto;

import java.time.OffsetDateTime;

/**
 * Structured error response for API consumers.
 * Backward compatible with GenericMessageResponse by including the 'message' field.
 */
public record ApiErrorResponse(
    int status,
    String code,
    String message,
    String path,
    OffsetDateTime timestamp
) {}

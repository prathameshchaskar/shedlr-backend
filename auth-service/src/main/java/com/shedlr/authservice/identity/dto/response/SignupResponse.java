package com.shedlr.authservice.identity.dto.response;

import java.util.UUID;

/**
 * SignupResponse provides feedback after registration and includes an identifier
 * for the pending account to facilitate self-service recovery (e.g., email typo correction).
 */
public record SignupResponse(
    String message,
    UUID pendingAccountId
) {}

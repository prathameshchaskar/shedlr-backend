package com.shedlr.authservice.identity.dto.request;

import jakarta.validation.constraints.*;
import java.util.UUID;

/**
 * UpdatePendingEmailRequest is used to fix an email typo during the verification phase.
 */
public record UpdatePendingEmailRequest(
    /** The unique identifier for the pending account, typically a UUID. */
    @NotNull(message = "Pending account ID is required")
    UUID pendingAccountId,

    /** The new, correct email address. */
    @NotBlank(message = "New email is required")
    @Email(message = "Invalid email format")
    @Size(max = 320)
    String newEmail
) {}

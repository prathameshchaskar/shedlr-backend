package com.shedlr.authservice.identity.enumtype;

/**
 * UserStatus represents the lifecycle state of a user account.
 * Using an enum ensures type safety and restricts values to a predefined set.
 */
public enum UserStatus {
    /** Account created but email not yet verified. */
    PENDING_VERIFICATION,
    /** Active and fully functional account. */
    ACTIVE,
    /** Temporarily locked due to too many failed login attempts. */
    LOCKED,
    /** Manually disabled by admin or system policy. */
    DISABLED,
    /** Soft-deleted account. */
    DELETED
}

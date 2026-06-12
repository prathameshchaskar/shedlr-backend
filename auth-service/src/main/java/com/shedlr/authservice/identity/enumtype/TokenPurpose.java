package com.shedlr.authservice.identity.enumtype;

/**
 * TokenPurpose categorizes security tokens for different auth-related flows.
 */
public enum TokenPurpose {
    /** Token used for verifying a user's email address after signup. */
    EMAIL_VERIFICATION,
    /** Token used for resetting a forgotten password. */
    PASSWORD_RESET
}

package com.shedlr.authservice.identity.enumtype;

/**
 * SessionStatus tracks the validity of a user session or refresh token.
 */
public enum SessionStatus {
    /** Session is active and can be used to obtain new access tokens. */
    ACTIVE,
    /** Session was manually terminated by the user (logout). */
    REVOKED,
    /** Session has passed its validity period. */
    EXPIRED
}

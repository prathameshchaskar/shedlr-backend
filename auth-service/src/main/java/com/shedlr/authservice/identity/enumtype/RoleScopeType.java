package com.shedlr.authservice.identity.enumtype;

/**
 * RoleScopeType determines the level at which a role is applied.
 * This supports multi-tenancy and granular project-level permissions.
 */
public enum RoleScopeType {
    /** Role applies to the entire workspace. */
    WORKSPACE,
    /** Role applies only to a specific project within a workspace. */
    PROJECT
}

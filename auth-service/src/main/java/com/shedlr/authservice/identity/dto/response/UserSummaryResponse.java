package com.shedlr.authservice.identity.dto.response;

import java.util.List;

/**
 * UserSummaryResponse provides a high-level overview of a user account.
 * Used for displaying user info in lists or as part of an auth response.
 */
public record UserSummaryResponse(
    Long id,
    String email,
    String fullName,
    boolean emailVerified,
    String status,
    List<String> roles,
    List<WorkspaceMembershipResponse> workspaces
) {}

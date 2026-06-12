package com.shedlr.authservice.identity.dto.response;

/**
 * WorkspaceMembershipResponse provides summary info about a user's workspace membership.
 */
public record WorkspaceMembershipResponse(
    Long workspaceId,
    String workspaceName,
    String workspaceCode
) {}

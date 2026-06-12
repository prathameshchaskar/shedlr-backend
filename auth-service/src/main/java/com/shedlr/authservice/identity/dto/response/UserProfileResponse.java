package com.shedlr.authservice.identity.dto.response;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * UserProfileResponse provides detailed information about a user, including profile metadata.
 */
public record UserProfileResponse(
    Long id,
    String email,
    String fullName,
    boolean emailVerified,
    OffsetDateTime emailVerifiedAt,
    String avatarUrl,
    String jobTitle,
    String timeZone,
    String locale,
    boolean emailNotificationsEnabled,
    boolean inAppNotificationsEnabled,
    List<String> linkedProviders,
    List<String> roles
) {}

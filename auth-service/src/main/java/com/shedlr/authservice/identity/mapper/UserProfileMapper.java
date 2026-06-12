package com.shedlr.authservice.identity.mapper;

import com.shedlr.authservice.identity.dto.response.UserProfileResponse;
import com.shedlr.authservice.identity.entity.UserAccount;
import com.shedlr.authservice.identity.entity.UserProfile;
import java.util.List;

/**
 * UserProfileMapper handles conversion between identity entities and their response DTOs.
 * Keeping mapping logic out of entities and controllers promotes separation of concerns.
 */
public final class UserProfileMapper {

    private UserProfileMapper() {
        // Private constructor to prevent instantiation of utility class
    }

    /**
     * Maps UserAccount and UserProfile entities to a UserProfileResponse.
     * Handles cases where profile might be null by providing default values.
     *
     * @param user The core user account entity.
     * @param profile The optional user profile entity.
     * @param linkedProviders List of active OAuth providers for this account.
     * @param roles List of roles assigned to the user.
     * @return A populated UserProfileResponse DTO.
     */
    public static UserProfileResponse toResponse(
        UserAccount user,
        UserProfile profile,
        List<String> linkedProviders,
        List<String> roles
    ) {
        return new UserProfileResponse(
            user.getId(),
            user.getEmail(),
            user.getFullName(),
            user.isEmailVerified(),
            user.getEmailVerifiedAt(),
            user.getAvatarUrl(),
            profile != null ? profile.getJobTitle() : null,
            profile != null ? profile.getTimeZone() : null,
            profile != null ? profile.getLocale() : null,
            profile != null && profile.isEmailNotificationsEnabled(),
            profile != null && profile.isInAppNotificationsEnabled(),
            linkedProviders,
            roles
        );
    }
}

package com.shedlr.authservice.identity.service;

import com.shedlr.authservice.identity.dto.request.UpdateProfileRequest;
import com.shedlr.authservice.identity.dto.response.UserProfileResponse;
import com.shedlr.authservice.identity.entity.UserAccount;
import com.shedlr.authservice.identity.entity.UserProfile;
import com.shedlr.authservice.identity.mapper.UserProfileMapper;
import com.shedlr.authservice.identity.repository.RoleAssignmentRepository;
import com.shedlr.authservice.identity.repository.UserAccountRepository;
import com.shedlr.authservice.identity.repository.UserProfileRepository;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UserProfileService manages user profile information and account-related metadata.
 * It provides methods to retrieve and update user details, ensuring separation between identity and profile.
 */
@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserAccountRepository userAccountRepository;
    private final UserProfileRepository userProfileRepository;
    private final RoleAssignmentRepository roleAssignmentRepository;

    /**
     * Retrieves the profile and account details for a user by their email.
     *
     * @param email The user's email address.
     * @return UserProfileResponse DTO containing combined account and profile data.
     */
    @Transactional(readOnly = true)
    public UserProfileResponse getProfileByEmail(String email) {
        UserAccount user = userAccountRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        UserProfile profile = userProfileRepository.findById(user.getId()).orElse(null);

        // Fetch roles and linked providers (ExternalIdentities) - simplified here
        List<String> roles = roleAssignmentRepository.findByUserIdAndActiveTrue(user.getId())
                .stream()
                .map(ra -> ra.getRoleType().name())
                .toList();

        // Placeholder for linked providers as it's not implemented in repository yet
        List<String> linkedProviders = Collections.emptyList();

        return UserProfileMapper.toResponse(user, profile, linkedProviders, roles);
    }

    /**
     * Updates the profile information for a user.
     *
     * @param email The user's email address.
     * @param request UpdateProfileRequest DTO containing new profile values.
     * @return The updated UserProfileResponse.
     */
    @Transactional
    public UserProfileResponse updateProfile(String email, UpdateProfileRequest request) {
        UserAccount user = userAccountRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        // Update UserAccount fields
        user.setFullName(request.fullName());
        if (request.avatarUrl() != null) {
            user.setAvatarUrl(request.avatarUrl());
        }
        userAccountRepository.save(user);

        // Update or create UserProfile fields
        UserProfile profile = userProfileRepository.findById(user.getId())
                .orElseGet(() -> {
                    UserProfile newProfile = new UserProfile();
                    newProfile.setUser(user);
                    return newProfile;
                });

        profile.setJobTitle(request.jobTitle());
        profile.setTimeZone(request.timeZone());
        profile.setLocale(request.locale());
        if (request.emailNotificationsEnabled() != null) {
            profile.setEmailNotificationsEnabled(request.emailNotificationsEnabled());
        }
        if (request.inAppNotificationsEnabled() != null) {
            profile.setInAppNotificationsEnabled(request.inAppNotificationsEnabled());
        }

        userProfileRepository.save(profile);

        // Re-fetch roles and providers for the response
        List<String> roles = roleAssignmentRepository.findByUserIdAndActiveTrue(user.getId())
                .stream()
                .map(ra -> ra.getRoleType().name())
                .toList();

        return UserProfileMapper.toResponse(user, profile, Collections.emptyList(), roles);
    }
}

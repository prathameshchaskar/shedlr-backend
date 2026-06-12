package com.shedlr.authservice.identity.controller;

import com.shedlr.authservice.identity.dto.request.UpdateProfileRequest;
import com.shedlr.authservice.identity.dto.response.UserProfileResponse;
import com.shedlr.authservice.identity.service.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * UserProfileController provides endpoints for managing the authenticated user's profile.
 * All endpoints here are protected and require a valid JWT.
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User Profile", description = "Endpoints for managing user personal information and preferences")
public class UserProfileController {

    private final UserProfileService userProfileService;

    /**
     * Retrieves the current authenticated user's profile.
     *
     * @param userDetails The authenticated user's principal details.
     * @return UserProfileResponse DTO.
     */
    @GetMapping("/me")
    @Operation(
            summary = "Get current user profile",
            description = "Retrieves the profile information of the currently authenticated user",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<UserProfileResponse> getCurrentUserProfile(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(userProfileService.getProfileByEmail(userDetails.getUsername()));
    }

    /**
     * Updates the current authenticated user's profile.
     *
     * @param userDetails The authenticated user's principal details.
     * @param request UpdateProfileRequest DTO.
     * @return The updated UserProfileResponse DTO.
     */
    @PutMapping("/me")
    @Operation(
            summary = "Update current user profile",
            description = "Updates personal details and preferences for the authenticated user",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<UserProfileResponse> updateCurrentUserProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        return ResponseEntity.ok(userProfileService.updateProfile(userDetails.getUsername(), request));
    }
}

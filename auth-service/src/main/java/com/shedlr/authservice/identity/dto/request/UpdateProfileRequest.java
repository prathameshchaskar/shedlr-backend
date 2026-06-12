package com.shedlr.authservice.identity.dto.request;

import jakarta.validation.constraints.*;

/**
 * UpdateProfileRequest contains data for updating user profile and preferences.
 */
public record UpdateProfileRequest(
    /** Updated full name. */
    @NotBlank(message = "Full name is required") 
    @Size(max = 150) 
    String fullName,

    /** Updated profile picture URL. */
    @Size(max = 500) 
    String avatarUrl,

    /** Updated professional title. */
    @Size(max = 100) 
    String jobTitle,

    /** Updated time zone preference. */
    @Size(max = 60) 
    String timeZone,

    /** Updated locale preference. */
    @Size(max = 20) 
    String locale,

    /** Toggle for email notifications. */
    Boolean emailNotificationsEnabled,

    /** Toggle for in-app notifications. */
    Boolean inAppNotificationsEnabled
) {}

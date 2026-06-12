package com.shedlr.authservice.identity.entity;

import com.shedlr.authservice.common.audit.AuditableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * UserProfile stores non-security related user metadata and preferences.
 * Uses a one-to-one relationship with UserAccount sharing the same primary key.
 */
@Entity
@Table(name = "user_profile")
@Getter
@Setter
public class UserProfile extends AuditableEntity {

    /** 
     * Shared primary key with UserAccount. 
     * This avoids an extra ID column and enforces the 1:1 constraint at the DB level.
     */
    @Id
    @Column(name = "user_id")
    private Long userId;

    /** Reference back to the UserAccount entity. */
    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private UserAccount user;

    /** Professional title of the user. */
    @Column(name = "job_title", length = 100)
    private String jobTitle;

    /** Preferred time zone for displaying timestamps in the UI (e.g., 'UTC', 'America/New_York'). */
    @Column(name = "time_zone", length = 60)
    private String timeZone;

    /** Preferred locale for internationalization (e.g., 'en-US', 'fr-FR'). */
    @Column(name = "locale", length = 20)
    private String locale;

    /** Global flag for opting in/out of email-based notifications. */
    @Column(name = "email_notifications_enabled", nullable = false)
    private boolean emailNotificationsEnabled;

    /** Global flag for opting in/out of in-app/push notifications. */
    @Column(name = "in_app_notifications_enabled", nullable = false)
    private boolean inAppNotificationsEnabled;

}

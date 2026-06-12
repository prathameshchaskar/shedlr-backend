package com.shedlr.authservice.identity.entity;

import com.shedlr.authservice.common.audit.AuditableEntity;
import com.shedlr.authservice.identity.enumtype.UserStatus;
import com.shedlr.authservice.workspace.entity.Workspace;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Collections;

/**
 * UserAccount stores identity, credentials, and security state.
 * It is separated from UserProfile to keep the core auth entity lean.
 * Implements UserDetails to integrate with Spring Security.
 */
@Entity
@Table(
    name = "user_account",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_account_email", columnNames = "email")
    },
    indexes = {
        @Index(name = "idx_user_account_workspace_id", columnList = "workspace_id"),
        @Index(name = "idx_user_account_status", columnList = "status"),
        @Index(name = "idx_user_account_email_verified", columnList = "email_verified")
    }
)
@Getter
@Setter
public class UserAccount extends AuditableEntity implements UserDetails {

    /** Primary key for the user. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Every user is associated with a workspace (tenant). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id")
    private Workspace workspace;

    /** Email used for login and notifications. Must be unique. */
    @Column(name = "email", nullable = false, length = 320)
    private String email;

    /** Argon2 or BCrypt hashed password. Nullable if user uses OAuth exclusively. */
    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    /** Full legal or display name of the user. */
    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    /** Current lifecycle state of the account. */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private UserStatus status;

    /** Flag indicating if the user has confirmed their email address. */
    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified;

    /** Timestamp when email was verified. */
    @Column(name = "email_verified_at")
    private OffsetDateTime emailVerifiedAt;

    /** Timestamp of the last successful login. */
    @Column(name = "last_login_at")
    private OffsetDateTime lastLoginAt;

    /** Counter for consecutive failed login attempts, used for locking. */
    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts;

    /** If the account is locked, this defines when it will be unlocked. */
    @Column(name = "locked_until")
    private OffsetDateTime lockedUntil;

    /** Security flag requiring the user to change their password on next login. */
    @Column(name = "must_change_password", nullable = false)
    private boolean mustChangePassword;

    /** Public URL for the user's profile picture. */
    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Roles will be fetched from RoleAssignment in a real implementation
        return Collections.emptyList();
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return status != UserStatus.DELETED;
    }

    @Override
    public boolean isAccountNonLocked() {
        return status != UserStatus.LOCKED &&
                (lockedUntil == null || lockedUntil.isBefore(OffsetDateTime.now()));
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return status == UserStatus.ACTIVE || status == UserStatus.PENDING_VERIFICATION;
    }
}

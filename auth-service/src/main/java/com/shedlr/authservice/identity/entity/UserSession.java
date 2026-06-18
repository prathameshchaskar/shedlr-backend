package com.shedlr.authservice.identity.entity;

import com.shedlr.authservice.common.audit.AuditableEntity;
import com.shedlr.authservice.identity.enumtype.SessionStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * UserSession tracks active refresh tokens and device metadata.
 * Enables features like session revocation, "logout from all devices", and refresh token rotation.
 */
@Entity
@Table(
    name = "user_session",
    indexes = {
        @Index(name = "idx_user_session_user_id", columnList = "user_id"),
        @Index(name = "idx_user_session_status", columnList = "status"),
        @Index(name = "idx_user_session_expires_at", columnList = "expires_at")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_session_refresh_token_hash", columnNames = "refresh_token_hash")
    }
)
@Getter
@Setter
public class UserSession extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Public identifier for fast indexed lookup. */
    @Column(name = "session_public_id", nullable = false, unique = true)
    private UUID sessionPublicId = UUID.randomUUID();

    /** The user who owns this session. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    /** 
     * Secure hash of the refresh token. 
     * Never store raw refresh tokens in the database.
     */
    @Column(name = "refresh_token_hash", nullable = false, length = 255)
    private String refreshTokenHash;

    /** Optional device identifier or name (e.g., 'iPhone 15', 'Chrome on Windows'). */
    @Column(name = "device_name", length = 120)
    private String deviceName;

    /** IP address where the session was initiated. */
    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    /** User agent string of the browser/client. */
    @Column(name = "user_agent", length = 500)
    private String userAgent;

    /** Current state of the session. */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SessionStatus status;

    /** Absolute expiration time for the session/refresh token. */
    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    /** Timestamp when the session was revoked (if status = REVOKED). */
    @Column(name = "revoked_at")
    private OffsetDateTime revokedAt;

}

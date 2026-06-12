package com.shedlr.authservice.identity.entity;

import com.shedlr.authservice.common.audit.AuditableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.OffsetDateTime;

/**
 * PasswordResetToken stores tokens for the "Forgot Password" flow.
 * Dedicated table allows for independent auditing and revocation of reset requests.
 */
@Entity
@Table(
    name = "password_reset_token",
    indexes = {
        @Index(name = "idx_password_reset_token_user_id", columnList = "user_id"),
        @Index(name = "idx_password_reset_token_expires_at", columnList = "expires_at")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_password_reset_token_token_hash", columnNames = "token_hash")
    }
)
@Getter
@Setter
public class PasswordResetToken extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The user who requested the password reset. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    /** Secure hash of the reset token. */
    @Column(name = "token_hash", nullable = false, length = 255)
    private String tokenHash;

    /** Expiration timestamp for the reset link. */
    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    /** Timestamp when the password was successfully reset using this token. */
    @Column(name = "used_at")
    private OffsetDateTime usedAt;

    /** IP address of the requester, for security auditing and rate limiting. */
    @Column(name = "requested_from_ip", length = 64)
    private String requestedFromIp;

    /** User agent of the requester, helps in identifying suspicious activity. */
    @Column(name = "requested_user_agent", length = 500)
    private String requestedUserAgent;

}

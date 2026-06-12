package com.shedlr.authservice.identity.entity;

import com.shedlr.authservice.common.audit.AuditableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.OffsetDateTime;

/**
 * EmailVerificationToken stores short-lived tokens for email verification.
 * Tokens are hashed before storage to prevent exploitation if the DB is compromised.
 */
@Entity
@Table(
    name = "email_verification_token",
    indexes = {
        @Index(name = "idx_email_verification_token_user_id", columnList = "user_id"),
        @Index(name = "idx_email_verification_token_expires_at", columnList = "expires_at")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_email_verification_token_token_hash", columnNames = "token_hash")
    }
)
@Getter
@Setter
public class EmailVerificationToken extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The user associated with this verification request. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    /** Secure hash of the random token sent to the user. */
    @Column(name = "token_hash", nullable = false, length = 255)
    private String tokenHash;

    /** Expiration timestamp for the token. */
    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    /** Timestamp when the token was successfully used. */
    @Column(name = "used_at")
    private OffsetDateTime usedAt;

    /** 
     * The email address this token was sent to. 
     * Useful for auditing and preventing race conditions if user changes email during verification.
     */
    @Column(name = "sent_to_email", nullable = false, length = 320)
    private String sentToEmail;

}

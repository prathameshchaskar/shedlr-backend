package com.shedlr.authservice.identity.entity;

import com.shedlr.authservice.common.audit.AuditableEntity;
import com.shedlr.authservice.identity.enumtype.ExternalProvider;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * ExternalIdentity links a local UserAccount to an external OAuth2 provider (e.g., Google).
 * This allows users to sign in via third-party services while maintaining a local profile.
 */
@Entity
@Table(
    name = "external_identity",
    indexes = {
        @Index(name = "idx_external_identity_user_id", columnList = "user_id")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_external_identity_provider_subject", columnNames = {"provider", "provider_subject"})
    }
)
@Getter
@Setter
public class ExternalIdentity extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The local account this external identity is linked to. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    /** The identity provider (e.g., GOOGLE). */
    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 30)
    private ExternalProvider provider;

    /** 
     * The unique identifier (subject) provided by the external service for this user. 
     * For Google, this is usually the 'sub' claim in the ID token.
     */
    @Column(name = "provider_subject", nullable = false, length = 255)
    private String providerSubject;

    /** Email address associated with the external account. */
    @Column(name = "provider_email", length = 320)
    private String providerEmail;

    /** Flag indicating if the email was verified by the external provider. */
    @Column(name = "provider_email_verified", nullable = false)
    private boolean providerEmailVerified;

}

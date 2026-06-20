package com.shedlr.authservice.identity.repository;

import com.shedlr.authservice.identity.entity.EmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for email verification tokens.
 */
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {

    /**
     * Find a token by its hash.
     * Tokens are looked up using the hash of the token provided by the user in the URL.
     */
    Optional<EmailVerificationToken> findByTokenHash(String tokenHash);

    /**
     * Find a token by its public UUID.
     */
    Optional<EmailVerificationToken> findByTokenId(UUID tokenId);

    /** Find any active tokens for a user to invalidate them if a new one is requested. */
    Optional<EmailVerificationToken> findByUserIdAndUsedAtIsNull(Long userId);

    /**
     * Bulk invalidates all active verification tokens for a specific user.
     * Used to ensure a clean token lifecycle.
     */
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE EmailVerificationToken t SET t.usedAt = :now WHERE t.user.id = :userId AND t.usedAt IS NULL")
    void invalidateAllActiveTokens(@org.springframework.data.repository.query.Param("userId") Long userId, @org.springframework.data.repository.query.Param("now") java.time.OffsetDateTime now);
}

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
}

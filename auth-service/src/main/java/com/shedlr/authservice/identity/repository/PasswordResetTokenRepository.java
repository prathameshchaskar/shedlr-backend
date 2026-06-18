package com.shedlr.authservice.identity.repository;

import com.shedlr.authservice.identity.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for password reset tokens.
 */
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    /**
     * Find a token by its hash.
     */
    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    /**
     * Find a reset token by its public UUID.
     */
    Optional<PasswordResetToken> findByTokenId(UUID tokenId);

    /** Invalidate existing tokens when a new one is requested. */
    void deleteByUserIdAndUsedAtIsNull(Long userId);
}

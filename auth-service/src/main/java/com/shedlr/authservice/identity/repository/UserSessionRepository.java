package com.shedlr.authservice.identity.repository;

import com.shedlr.authservice.identity.entity.UserSession;
import com.shedlr.authservice.identity.enumtype.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;
import java.util.UUID;

/**
 * Repository for managing user sessions and refresh tokens.
 */
public interface UserSessionRepository extends JpaRepository<UserSession, Long> {

    /**
     * Find an active session by refresh token hash.
     */
    Optional<UserSession> findByRefreshTokenHashAndStatus(String refreshTokenHash, SessionStatus status);

    /**
     * Find an active session by its public public ID.
     */
    Optional<UserSession> findBySessionPublicIdAndStatus(UUID sessionPublicId, SessionStatus status);

    /** Find all active sessions for a user (e.g., to list logged-in devices). */
    List<UserSession> findByUserIdAndStatus(Long userId, SessionStatus status);
}

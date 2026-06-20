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
     * Find a session by its public public ID, regardless of status.
     * Used for rotation reuse detection.
     */
    Optional<UserSession> findBySessionPublicId(UUID sessionPublicId);

    /**
     * Revokes all sessions belonging to a specific family ID.
     * Triggered when refresh token reuse is detected.
     */
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE UserSession s SET s.status = 'REVOKED', s.revokedAt = :now WHERE s.familyId = :familyId AND s.status = 'ACTIVE'")
    void revokeFamily(@org.springframework.data.repository.query.Param("familyId") UUID familyId, @org.springframework.data.repository.query.Param("now") java.time.OffsetDateTime now);

    /** Find all active sessions for a user (e.g., to list logged-in devices). */
    List<UserSession> findByUserIdAndStatus(Long userId, SessionStatus status);
}

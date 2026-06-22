package com.shedlr.authservice.identity.service;

import com.shedlr.authservice.common.config.SecurityProperties;
import com.shedlr.authservice.identity.entity.UserAccount;
import com.shedlr.authservice.identity.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/**
 * LoginAuditService handles security-sensitive updates related to the login process.
 * Methods use Propagation.REQUIRES_NEW to ensure that audit data (like failed attempts)
 * is persisted even if the main authentication transaction rolls back.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LoginAuditService {

    private final UserAccountRepository userAccountRepository;
    private final SecurityProperties securityProperties;

    /**
     * Handles failed login attempts and triggers account lockout if the threshold is reached.
     *
     * @param email The email of the user who failed to login.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleFailedLogin(String email) {
        userAccountRepository.findByEmail(email).ifPresent(user -> {
            int attempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(attempts);

            int maxAttempts = securityProperties.getLockout().getMaxAttempts();
            if (attempts >= maxAttempts) {
                user.setLockedUntil(OffsetDateTime.now().plusMinutes(securityProperties.getLockout().getDurationMinutes()));
                log.warn("Account locked for user {} due to {} failed attempts. Locked until: {}",
                        email, attempts, user.getLockedUntil());
            } else {
                log.info("Failed login attempt {} for user: {}", attempts, email);
            }
            userAccountRepository.save(user);
        });
    }

    /**
     * Resets failed login attempts and clears lockout status for a user.
     *
     * @param userId The ID of the user whose attempts should be reset.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void resetFailedAttempts(Long userId) {
        userAccountRepository.findById(userId).ifPresent(user -> {
            if (user.getFailedLoginAttempts() > 0 || user.getLockedUntil() != null) {
                user.setFailedLoginAttempts(0);
                user.setLockedUntil(null);
                userAccountRepository.save(user);
                log.info("Failed login attempts reset for user: {}", user.getEmail());
            }
        });
    }
}

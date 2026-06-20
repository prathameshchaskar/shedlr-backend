package com.shedlr.authservice.identity.service;

import com.shedlr.authservice.common.config.SecurityProperties;
import com.shedlr.authservice.common.exception.base.AuthException;
import com.shedlr.authservice.common.exception.errorcode.ErrorCode;
import com.shedlr.authservice.identity.dto.request.*;
import com.shedlr.authservice.identity.dto.response.*;
import com.shedlr.authservice.identity.entity.*;
import com.shedlr.authservice.identity.enumtype.SessionStatus;
import com.shedlr.authservice.identity.enumtype.UserStatus;
import com.shedlr.authservice.identity.repository.*;
import com.shedlr.authservice.identity.security.JwtService;
import com.shedlr.authservice.workspace.entity.Workspace;
import com.shedlr.authservice.workspace.repository.WorkspaceRepository;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * AuthService implements core authentication business logic.
 * It coordinates between repositories, security components, and DTOs.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserAccountRepository userAccountRepository;
    private final WorkspaceRepository workspaceRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final UserSessionRepository userSessionRepository;
    private final RoleAssignmentRepository roleAssignmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final HttpServletRequest httpServletRequest;
    private final SecurityProperties securityProperties;

    /**
     * Handles new user registration.
     * Enforces email uniqueness and initializes account in pending state.
     *
     * @param request SignupRequest containing user details.
     * @return SignupResponse indicating success and providing the pending account identifier.
     */
    @Transactional
    public SignupResponse signup(SignupRequest request) {
        log.info("Processing signup request for email: {}", request.email());

        if (!request.password().equals(request.confirmPassword())) {
            throw new AuthException(ErrorCode.SIGNUP_PASSWORD_MISMATCH);
        }

        if (userAccountRepository.existsByEmail(request.email())) {
            log.warn("Signup failed: Email {} is already registered", request.email());
            throw new AuthException(ErrorCode.SIGNUP_EMAIL_EXISTS);
        }

        // Multi-tenant best practice: Every user must belong to a workspace.
        // We ensure a 'DEFAULT' workspace exists for public signups.
        Workspace workspace = workspaceRepository.findByCode("DEFAULT")
                .orElseGet(() -> {
                    log.info("Default workspace not found. Creating a new one.");
                    Workspace ws = new Workspace();
                    ws.setName("Default Workspace");
                    ws.setCode("DEFAULT");
                    ws.setStatus("ACTIVE");
                    return workspaceRepository.save(ws);
                });

        UserAccount user = new UserAccount();
        user.setFullName(request.fullName());
        user.setEmail(request.email().toLowerCase().trim());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setStatus(UserStatus.PENDING_VERIFICATION);
        user.setEmailVerified(false);
        user.setWorkspace(workspace); // Crucial: Fixes the 'user_account' 500 error due to null workspace
        
        userAccountRepository.save(user);
        log.info("User account created successfully for email: {}", user.getEmail());

        EmailVerificationToken token = sendVerificationToken(user);

        return new SignupResponse(
                "User registered successfully. Please verify your email.",
                token.getTokenId()
        );
    }

    /**
     * Allows a user with PENDING_VERIFICATION status to update their email address.
     * This is crucial for fixing typos during the registration phase.
     */
    @Transactional
    public GenericMessageResponse updatePendingEmail(UpdatePendingEmailRequest request) {
        log.info("Processing email update for pending account ID: {}", request.pendingAccountId());

        // 1. Identify the pending account via the current active verification token
        EmailVerificationToken activeToken = emailVerificationTokenRepository.findByTokenId(request.pendingAccountId())
                .filter(t -> t.getUsedAt() == null)
                .orElseThrow(() -> new AuthException(ErrorCode.VERIFY_TOKEN_INVALID, "Invalid or expired pending account session"));

        UserAccount user = activeToken.getUser();
        String oldEmail = user.getEmail();

        if (user.getStatus() != UserStatus.PENDING_VERIFICATION) {
            log.warn("Email update denied: User {} is not in PENDING_VERIFICATION status", oldEmail);
            throw new AuthException(ErrorCode.VALIDATION_ERROR, "Email can only be updated for unverified accounts.");
        }

        String newEmail = request.newEmail().toLowerCase().trim();
        if (userAccountRepository.existsByEmail(newEmail)) {
            log.warn("Email update failed: New email {} is already registered", newEmail);
            throw new AuthException(ErrorCode.SIGNUP_EMAIL_EXISTS, "The new email is already registered.");
        }

        // 2. Update UserAccount
        user.setEmail(newEmail);
        userAccountRepository.save(user);

        // 3. Invalidate all active verification tokens for this user
        invalidateAllActiveTokens(user.getId());

        // 4. Generate and send new verification token
        sendVerificationToken(user);

        // 5. Audit Logging
        String clientIp = httpServletRequest.getRemoteAddr();
        log.info("[AUDIT] Email updated for pending account. UserID: {}, OldEmail: {}, NewEmail: {}, IP: {}, Timestamp: {}",
                user.getId(), oldEmail, newEmail, clientIp, OffsetDateTime.now());

        log.info("Email updated and new verification sent for user ID: {}", user.getId());
        return new GenericMessageResponse("Email updated successfully. A new verification link has been sent.");
    }

    /**
     * Resends the verification email to the user.
     * Hardened against user enumeration: Returns generic success even if email is not found.
     */
    @Transactional
    public GenericMessageResponse resendVerificationEmail(ResendVerificationRequest request) {
        String email = request.email().toLowerCase().trim();
        log.info("Processing resend verification request for email: {}", email);

        userAccountRepository.findByEmail(email).ifPresentOrElse(
            user -> {
                if (user.isEmailVerified()) {
                    log.info("Resend verification ignored: Email {} is already verified", email);
                    // We don't throw VERIFY_ALREADY_DONE here to prevent enumeration
                    return;
                }

                // Invalidate old tokens using bulk update
                invalidateAllActiveTokens(user.getId());

                // Generate and send new token
                sendVerificationToken(user);
                log.info("New verification token sent to: {}", email);
            },
            () -> log.warn("Resend verification requested for non-existent email: {}", email)
        );

        // Always return the same generic message to prevent account enumeration
        return new GenericMessageResponse("If an account exists for that email, a verification email has been sent.");
    }

    private EmailVerificationToken sendVerificationToken(UserAccount user) {
        // Generate a high-entropy secret and a public ID
        String secret = UUID.randomUUID().toString();
        EmailVerificationToken verificationToken = new EmailVerificationToken();
        verificationToken.setUser(user);
        
        // Two-factor token format: <public_id>:<secret>
        // This allows O(1) DB lookup followed by secure hash verification.
        String publicToken = verificationToken.getTokenId().toString() + ":" + secret;
        
        verificationToken.setTokenHash(passwordEncoder.encode(secret));
        verificationToken.setExpiresAt(OffsetDateTime.now().plusHours(24));
        verificationToken.setSentToEmail(user.getEmail());
        EmailVerificationToken savedToken = emailVerificationTokenRepository.save(verificationToken);

        // Send combined token to user
        emailService.sendVerificationEmail(user.getEmail(), publicToken);
        
        return savedToken;
    }

    /**
     * Verifies user's email using the provided token.
     *
     * @param request VerifyEmailRequest containing the token.
     * @return GenericMessageResponse indicating verification result.
     */
    @Transactional
    public GenericMessageResponse verifyEmail(VerifyEmailRequest request) {
        log.info("Verifying email with token...");

        // 1. Parse two-factor token: <uuid>:<secret>
        String fullToken = request.token();
        if (fullToken == null || !fullToken.contains(":")) {
            throw new AuthException(ErrorCode.VERIFY_TOKEN_INVALID, "Invalid token format");
        }

        String[] parts = fullToken.split(":");
        UUID tokenId;
        try {
            tokenId = UUID.fromString(parts[0]);
        } catch (IllegalArgumentException e) {
            throw new AuthException(ErrorCode.VERIFY_TOKEN_INVALID, "Invalid token identifier");
        }
        String secret = parts[1];

        // 2. Lookup the token
        EmailVerificationToken token = emailVerificationTokenRepository.findByTokenId(tokenId)
                .orElseThrow(() -> new AuthException(ErrorCode.VERIFY_TOKEN_INVALID, "Invalid or expired token"));

        UserAccount user = token.getUser();

        // 3. IDEMPOTENCY CHECK: If user is already active, return success immediately
        if (user.getStatus() == UserStatus.ACTIVE && user.isEmailVerified()) {
            log.info("Email already verified for user: {}", user.getEmail());
            return new GenericMessageResponse("Email already verified. You can now log in.");
        }

        // 4. Validate token status
        if (token.getUsedAt() != null) {
            // This case is unlikely if the user is not ACTIVE yet, but handled for safety
            throw new AuthException(ErrorCode.VERIFY_TOKEN_REUSED);
        }

        if (token.getExpiresAt().isBefore(OffsetDateTime.now())) {
            log.warn("Email verification failed: Token expired for user {}", user.getEmail());
            throw new AuthException(ErrorCode.VERIFY_TOKEN_EXPIRED);
        }

        // 5. Secure verification of the secret hash
        if (!passwordEncoder.matches(secret, token.getTokenHash())) {
            log.warn("Email verification failed: Secret mismatch for token ID {}", tokenId);
            throw new AuthException(ErrorCode.VERIFY_TOKEN_INVALID, "Invalid or expired token");
        }

        // 6. Complete verification
        user.setEmailVerified(true);
        user.setEmailVerifiedAt(OffsetDateTime.now());
        user.setStatus(UserStatus.ACTIVE);
        userAccountRepository.save(user);

        token.setUsedAt(OffsetDateTime.now());
        emailVerificationTokenRepository.save(token);

        // 7. Cleanup: Invalidate any other active tokens for this user
        invalidateAllActiveTokens(user.getId());

        return new GenericMessageResponse("Email verified successfully. You can now log in.");
    }

    /**
     * Internal helper to bulk invalidate all active verification tokens for a user.
     */
    private void invalidateAllActiveTokens(Long userId) {
        emailVerificationTokenRepository.invalidateAllActiveTokens(userId, OffsetDateTime.now());
    }

    /**
     * Authenticates a user and returns JWT tokens.
     * Implements refresh token rotation for enhanced security.
     * Includes account lockout protection.
     *
     * @param request LoginRequest containing credentials.
     * @return AuthResponse containing access and refresh tokens.
     */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        String email = request.email().toLowerCase().trim();
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.password())
            );
        } catch (BadCredentialsException e) {
            handleFailedLogin(email);
            throw e;
        }

        UserAccount user = userAccountRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Reset failed login attempts upon successful authentication
        if (user.getFailedLoginAttempts() > 0 || user.getLockedUntil() != null) {
            resetFailedAttempts(user);
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(request.email());
        String jwtToken = jwtService.generateToken(userDetails);
        
        // Generate two-factor refresh token: <public_id>:<secret>
        UUID publicId = UUID.randomUUID();
        String secret = UUID.randomUUID().toString();
        String publicRefreshToken = publicId.toString() + ":" + secret;

        // Track and Rotate Session
        revokeAllUserSessions(user);
        
        String ipAddress = null;
        String userAgent = null;
        try {
            jakarta.servlet.http.HttpServletRequest httpRequest = ((org.springframework.web.context.request.ServletRequestAttributes) org.springframework.web.context.request.RequestContextHolder.currentRequestAttributes()).getRequest();
            ipAddress = httpRequest.getRemoteAddr();
            userAgent = httpRequest.getHeader("User-Agent");
        } catch (Exception e) {
            log.warn("Could not extract request details for audit logging during login");
        }
        
        saveUserSession(user, publicRefreshToken, UUID.randomUUID(), ipAddress, userAgent);

        // Map to UserSummaryResponse for complete production response
        UserSummaryResponse summary = getUserSummary(user);

        return new AuthResponse(
                jwtToken,
                publicRefreshToken,
                "Bearer",
                securityProperties.getJwt().getExpiration() / 1000,
                summary
        );
    }

    /**
     * Internal helper to reset failed login attempts for a user.
     */
    private void resetFailedAttempts(UserAccount user) {
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        userAccountRepository.save(user);
        log.info("Failed login attempts reset for user: {}", user.getEmail());
    }

    /**
     * Internal helper to handle failed login attempts and trigger lockout if threshold reached.
     */
    private void handleFailedLogin(String email) {
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
     * Refreshes the access token using a valid refresh token.
     * Implements refresh token rotation with reuse detection.
     */
    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request, String ipAddress, String userAgent) {
        // Parse two-factor token: <uuid>:<secret>
        String fullToken = request.refreshToken();
        if (fullToken == null || !fullToken.contains(":")) {
            throw new AuthException(ErrorCode.REFRESH_TOKEN_INVALID, "Invalid refresh token format");
        }

        String[] parts = fullToken.split(":");
        UUID sessionPublicId;
        try {
            sessionPublicId = UUID.fromString(parts[0]);
        } catch (IllegalArgumentException e) {
            throw new AuthException(ErrorCode.REFRESH_TOKEN_INVALID, "Invalid refresh token identifier");
        }
        String secret = parts[1];
        
        // Find session regardless of status to detect reuse
        UserSession session = userSessionRepository.findBySessionPublicId(sessionPublicId)
                .orElseThrow(() -> new AuthException(ErrorCode.REFRESH_TOKEN_INVALID, "Invalid or expired refresh token"));

        // 1. Detect Reuse (REVOKED status)
        if (session.getStatus() == SessionStatus.REVOKED) {
            log.error("SECURITY ALERT: Refresh token reuse detected! userId={}, familyId={}, sessionId={}, IP={}, UA={}", 
                    session.getUser().getId(), session.getFamilyId(), session.getSessionPublicId(), ipAddress, userAgent);
            
            // Revoke all sessions in the family immediately
            userSessionRepository.revokeFamily(session.getFamilyId(), OffsetDateTime.now());
            
            throw new AuthException(ErrorCode.REFRESH_TOKEN_REUSED);
        }

        // 2. Validate Active Session
        if (session.getStatus() != SessionStatus.ACTIVE) {
            throw new AuthException(ErrorCode.REFRESH_TOKEN_INVALID, "Invalid refresh token status");
        }

        // 3. Secure secret verification
        if (!passwordEncoder.matches(secret, session.getRefreshTokenHash())) {
            log.warn("Refresh token failed: Secret mismatch for session {}", sessionPublicId);
            throw new AuthException(ErrorCode.REFRESH_TOKEN_INVALID, "Invalid or expired refresh token");
        }

        // 4. Expiration check
        if (session.getExpiresAt().isBefore(OffsetDateTime.now())) {
            session.setStatus(SessionStatus.EXPIRED);
            userSessionRepository.save(session);
            throw new AuthException(ErrorCode.REFRESH_TOKEN_EXPIRED);
        }

        UserAccount user = session.getUser();
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        
        String newAccessToken = jwtService.generateToken(userDetails);
        
        // Generate new two-factor refresh token for rotation
        UUID nextPublicId = UUID.randomUUID();
        String nextSecret = UUID.randomUUID().toString();
        String nextPublicRefreshToken = nextPublicId.toString() + ":" + nextSecret;

        // Rotate Refresh Token: Revoke current session and create a new one in the same family
        session.setStatus(SessionStatus.REVOKED);
        session.setRevokedAt(OffsetDateTime.now());
        userSessionRepository.save(session);

        saveUserSession(user, nextPublicRefreshToken, session.getFamilyId(), ipAddress, userAgent);

        return new AuthResponse(
                newAccessToken,
                nextPublicRefreshToken,
                "Bearer",
                3600,
                getUserSummary(user)
        );
    }

    /**
     * Revokes all sessions for the user (Logout).
     */
    @Transactional
    public void logout(String email) {
        UserAccount user = userAccountRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        revokeAllUserSessions(user);
    }

    private UserSummaryResponse getUserSummary(UserAccount user) {
        var roleAssignments = roleAssignmentRepository.findByUserIdAndActiveTrue(user.getId());
        
        List<String> roles = roleAssignments.stream()
                .map(ra -> ra.getRoleType().name())
                .distinct()
                .toList();
        
        List<WorkspaceMembershipResponse> workspaces = roleAssignments.stream()
                .map(ra -> new WorkspaceMembershipResponse(
                        ra.getWorkspace().getId(),
                        ra.getWorkspace().getName(),
                        ra.getWorkspace().getCode()
                ))
                .distinct()
                .toList();

        return new UserSummaryResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.isEmailVerified(),
                user.getStatus().name(),
                roles,
                workspaces
        );
    }

    private void saveUserSession(UserAccount user, String refreshToken, UUID familyId, String ipAddress, String userAgent) {
        // Parse public component if rotation is in progress
        String secret = refreshToken;
        UUID publicId = null;

        if (refreshToken.contains(":")) {
            String[] parts = refreshToken.split(":");
            publicId = UUID.fromString(parts[0]);
            secret = parts[1];
        }

        UserSession session = new UserSession();
        session.setUser(user);
        session.setFamilyId(familyId);
        session.setIpAddress(ipAddress);
        session.setUserAgent(userAgent);
        if (publicId != null) {
            session.setSessionPublicId(publicId);
        }
        session.setRefreshTokenHash(passwordEncoder.encode(secret));
        session.setStatus(SessionStatus.ACTIVE);
        session.setExpiresAt(OffsetDateTime.now().plusDays(7));
        userSessionRepository.save(session);
    }

    private void revokeAllUserSessions(UserAccount user) {
        List<UserSession> activeSessions = userSessionRepository.findByUserIdAndStatus(user.getId(), SessionStatus.ACTIVE);

        if (activeSessions.isEmpty()) return;

        activeSessions.forEach(session -> {
            session.setStatus(SessionStatus.REVOKED);
            session.setRevokedAt(OffsetDateTime.now());
        });
        userSessionRepository.saveAll(activeSessions);
    }

    /**
     * Initiates password reset flow by sending a reset email.
     *
     * @param request ForgotPasswordRequest containing user email.
     * @return GenericMessageResponse (always success for security).
     */
    @Transactional
    public GenericMessageResponse forgotPassword(ForgotPasswordRequest request) {
        userAccountRepository.findByEmail(request.email().toLowerCase().trim())
                .ifPresent(user -> {
                    String secret = UUID.randomUUID().toString();
                    PasswordResetToken resetToken = new PasswordResetToken();
                    resetToken.setUser(user);
                    
                    String publicToken = resetToken.getTokenId().toString() + ":" + secret;
                    
                    resetToken.setTokenHash(passwordEncoder.encode(secret));
                    resetToken.setExpiresAt(OffsetDateTime.now().plusMinutes(30));
                    passwordResetTokenRepository.save(resetToken);

                    emailService.sendPasswordResetEmail(user.getEmail(), publicToken);
                });

        return new GenericMessageResponse("If an account exists for this email, a reset link has been sent.");
    }

    /**
     * Resets the user's password using the provided token.
     *
     * @param request ResetPasswordRequest containing token and new password.
     * @return GenericMessageResponse indicating success.
     */
    @Transactional
    public GenericMessageResponse resetPassword(ResetPasswordRequest request) {
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new AuthException(ErrorCode.RESET_PASSWORD_MISMATCH);
        }

        // Parse two-factor token: <uuid>:<secret>
        String fullToken = request.token();
        if (fullToken == null || !fullToken.contains(":")) {
            throw new AuthException(ErrorCode.RESET_TOKEN_INVALID, "Invalid reset token format");
        }

        String[] parts = fullToken.split(":");
        UUID tokenId;
        try {
            tokenId = UUID.fromString(parts[0]);
        } catch (IllegalArgumentException e) {
            throw new AuthException(ErrorCode.RESET_TOKEN_INVALID, "Invalid reset token identifier");
        }
        String secret = parts[1];

        // O(1) Lookup
        PasswordResetToken token = passwordResetTokenRepository.findByTokenId(tokenId)
                .filter(t -> t.getUsedAt() == null)
                .orElseThrow(() -> new AuthException(ErrorCode.RESET_TOKEN_INVALID, "Invalid or expired token"));

        // Secure verification
        if (!passwordEncoder.matches(secret, token.getTokenHash())) {
            log.warn("Password reset failed: Secret mismatch for token ID {}", tokenId);
            throw new AuthException(ErrorCode.RESET_TOKEN_INVALID, "Invalid or expired token");
        }

        if (token.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new AuthException(ErrorCode.RESET_TOKEN_EXPIRED);
        }

        UserAccount user = token.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userAccountRepository.save(user);

        token.setUsedAt(OffsetDateTime.now());
        passwordResetTokenRepository.save(token);

        return new GenericMessageResponse("Password reset successfully.");
    }
}

package com.shedlr.authservice.identity.service;

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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
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

    /**
     * Handles new user registration.
     * Enforces email uniqueness and initializes account in pending state.
     *
     * @param request SignupRequest containing user details.
     * @return GenericMessageResponse indicating success.
     */
    @Transactional
    public GenericMessageResponse signup(SignupRequest request) {
        log.info("Processing signup request for email: {}", request.email());

        if (!request.password().equals(request.confirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }

        if (userAccountRepository.existsByEmail(request.email())) {
            log.warn("Signup failed: Email {} is already registered", request.email());
            throw new IllegalStateException("Email already registered");
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

        sendVerificationToken(user);

        return new GenericMessageResponse("User registered successfully. Please verify your email.");
    }

    /**
     * Resends the verification email to the user.
     */
    @Transactional
    public GenericMessageResponse resendVerificationEmail(ResendVerificationRequest request) {
        UserAccount user = userAccountRepository.findByEmail(request.email().toLowerCase().trim())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (user.isEmailVerified()) {
            throw new IllegalStateException("Email is already verified");
        }

        // Invalidate old tokens
        emailVerificationTokenRepository.findByUserIdAndUsedAtIsNull(user.getId())
                .ifPresent(token -> {
                    token.setUsedAt(OffsetDateTime.now()); // Mark as 'used' to effectively invalidate
                    emailVerificationTokenRepository.save(token);
                });

        sendVerificationToken(user);

        return new GenericMessageResponse("Verification email resent successfully.");
    }

    private void sendVerificationToken(UserAccount user) {
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
        emailVerificationTokenRepository.save(verificationToken);

        // Send combined token to user
        emailService.sendVerificationEmail(user.getEmail(), publicToken);
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

        // Parse two-factor token: <uuid>:<secret>
        String fullToken = request.token();
        if (fullToken == null || !fullToken.contains(":")) {
            throw new IllegalArgumentException("Invalid token format");
        }

        String[] parts = fullToken.split(":");
        UUID tokenId;
        try {
            tokenId = UUID.fromString(parts[0]);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid token identifier");
        }
        String secret = parts[1];

        // O(1) Lookup by indexed UUID
        EmailVerificationToken token = emailVerificationTokenRepository.findByTokenId(tokenId)
                .filter(t -> t.getUsedAt() == null)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired token"));

        // Secure verification of the secret hash
        if (!passwordEncoder.matches(secret, token.getTokenHash())) {
            log.warn("Email verification failed: Secret mismatch for token ID {}", tokenId);
            throw new IllegalArgumentException("Invalid or expired token");
        }

        if (token.getExpiresAt().isBefore(OffsetDateTime.now())) {
            log.warn("Email verification failed: Token expired for user {}", token.getUser().getEmail());
            throw new IllegalStateException("Token has expired");
        }

        UserAccount user = token.getUser();
        user.setEmailVerified(true);
        user.setEmailVerifiedAt(OffsetDateTime.now());
        user.setStatus(UserStatus.ACTIVE);
        userAccountRepository.save(user);

        token.setUsedAt(OffsetDateTime.now());
        emailVerificationTokenRepository.save(token);

        return new GenericMessageResponse("Email verified successfully. You can now log in.");
    }

    /**
     * Authenticates a user and returns JWT tokens.
     * Implements refresh token rotation for enhanced security.
     *
     * @param request LoginRequest containing credentials.
     * @return AuthResponse containing access and refresh tokens.
     */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email(),
                        request.password()
                )
        );

        UserAccount user = userAccountRepository.findByEmail(request.email().toLowerCase().trim())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        UserDetails userDetails = userDetailsService.loadUserByUsername(request.email());
        String jwtToken = jwtService.generateToken(userDetails);
        
        // Generate two-factor refresh token: <public_id>:<secret>
        UUID publicId = UUID.randomUUID();
        String secret = UUID.randomUUID().toString();
        String publicRefreshToken = publicId.toString() + ":" + secret;

        // Track and Rotate Session
        revokeAllUserSessions(user);
        saveUserSession(user, publicRefreshToken);

        // Map to UserSummaryResponse for complete production response
        UserSummaryResponse summary = getUserSummary(user);

        return new AuthResponse(
                jwtToken,
                publicRefreshToken,
                "Bearer",
                3600, // 1 hour
                summary
        );
    }

    /**
     * Refreshes the access token using a valid refresh token.
     * Implements refresh token rotation.
     */
    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        // Parse two-factor token: <uuid>:<secret>
        String fullToken = request.refreshToken();
        if (fullToken == null || !fullToken.contains(":")) {
            throw new IllegalArgumentException("Invalid refresh token format");
        }

        String[] parts = fullToken.split(":");
        UUID sessionPublicId;
        try {
            sessionPublicId = UUID.fromString(parts[0]);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid refresh token identifier");
        }
        String secret = parts[1];
        
        // O(1) Lookup by indexed UUID
        UserSession session = userSessionRepository.findBySessionPublicIdAndStatus(sessionPublicId, SessionStatus.ACTIVE)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired refresh token"));

        // Secure verification
        if (!passwordEncoder.matches(secret, session.getRefreshTokenHash())) {
            log.warn("Refresh token failed: Secret mismatch for session {}", sessionPublicId);
            throw new IllegalArgumentException("Invalid or expired refresh token");
        }

        if (session.getExpiresAt().isBefore(OffsetDateTime.now())) {
            session.setStatus(SessionStatus.EXPIRED);
            userSessionRepository.save(session);
            throw new IllegalStateException("Refresh token has expired");
        }

        UserAccount user = session.getUser();
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        
        String newAccessToken = jwtService.generateToken(userDetails);
        
        // Generate new two-factor refresh token for rotation
        UUID nextPublicId = UUID.randomUUID();
        String nextSecret = UUID.randomUUID().toString();
        String nextPublicRefreshToken = nextPublicId.toString() + ":" + nextSecret;

        // Rotate Refresh Token: Revoke current session and create a new one
        session.setStatus(SessionStatus.REVOKED);
        session.setRevokedAt(OffsetDateTime.now());
        userSessionRepository.save(session);

        saveUserSession(user, nextPublicRefreshToken);

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

    private void saveUserSession(UserAccount user, String refreshToken) {
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
            throw new IllegalArgumentException("Passwords do not match");
        }

        // Parse two-factor token: <uuid>:<secret>
        String fullToken = request.token();
        if (fullToken == null || !fullToken.contains(":")) {
            throw new IllegalArgumentException("Invalid reset token format");
        }

        String[] parts = fullToken.split(":");
        UUID tokenId;
        try {
            tokenId = UUID.fromString(parts[0]);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid reset token identifier");
        }
        String secret = parts[1];

        // O(1) Lookup
        PasswordResetToken token = passwordResetTokenRepository.findByTokenId(tokenId)
                .filter(t -> t.getUsedAt() == null)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired token"));

        // Secure verification
        if (!passwordEncoder.matches(secret, token.getTokenHash())) {
            log.warn("Password reset failed: Secret mismatch for token ID {}", tokenId);
            throw new IllegalArgumentException("Invalid or expired token");
        }

        if (token.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new IllegalStateException("Token has expired");
        }

        UserAccount user = token.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userAccountRepository.save(user);

        token.setUsedAt(OffsetDateTime.now());
        passwordResetTokenRepository.save(token);

        return new GenericMessageResponse("Password reset successfully.");
    }
}

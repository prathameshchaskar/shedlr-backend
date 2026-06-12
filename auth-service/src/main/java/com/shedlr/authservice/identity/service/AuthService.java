package com.shedlr.authservice.identity.service;

import com.shedlr.authservice.identity.dto.request.*;
import com.shedlr.authservice.identity.dto.response.*;
import com.shedlr.authservice.identity.entity.*;
import com.shedlr.authservice.identity.enumtype.SessionStatus;
import com.shedlr.authservice.identity.enumtype.UserStatus;
import com.shedlr.authservice.identity.repository.*;
import com.shedlr.authservice.identity.security.JwtService;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
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
public class AuthService {

    private final UserAccountRepository userAccountRepository;
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
        if (!request.password().equals(request.confirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }

        if (userAccountRepository.existsByEmail(request.email())) {
            throw new IllegalStateException("Email already registered");
        }

        UserAccount user = new UserAccount();
        user.setFullName(request.fullName());
        user.setEmail(request.email().toLowerCase().trim());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setStatus(UserStatus.PENDING_VERIFICATION);
        user.setEmailVerified(false);
        userAccountRepository.save(user);

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
        // Generate and save email verification token
        String token = UUID.randomUUID().toString();
        EmailVerificationToken verificationToken = new EmailVerificationToken();
        verificationToken.setUser(user);
        verificationToken.setTokenHash(passwordEncoder.encode(token));
        verificationToken.setExpiresAt(OffsetDateTime.now().plusHours(24));
        verificationToken.setSentToEmail(user.getEmail());
        emailVerificationTokenRepository.save(verificationToken);

        // Send verification email
        emailService.sendVerificationEmail(user.getEmail(), token);
    }

    /**
     * Verifies user's email using the provided token.
     *
     * @param request VerifyEmailRequest containing the token.
     * @return GenericMessageResponse indicating verification result.
     */
    @Transactional
    public GenericMessageResponse verifyEmail(VerifyEmailRequest request) {
        // In a real scenario, we'd need to find the token by hash. 
        // This is tricky because we can't reverse the hash.
        // Usually, we'd either store the token as a plain string (less secure) or
        // provide the token ID along with the raw token in the link.
        // For this implementation, let's assume we find it by some means or use a less secure lookup for demo.
        // PRODUCTION TIP: Use a separate non-hashed 'public_id' to find the record, then verify the hash.
        
        // Simulating finding by raw token for simplicity in this step
        EmailVerificationToken token = emailVerificationTokenRepository.findAll().stream()
                .filter(t -> t.getUsedAt() == null && passwordEncoder.matches(request.token(), t.getTokenHash()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired token"));

        if (token.getExpiresAt().isBefore(OffsetDateTime.now())) {
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
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        // Track and Rotate Session
        revokeAllUserSessions(user);
        saveUserSession(user, refreshToken);

        // Map to UserSummaryResponse for complete production response
        UserSummaryResponse summary = getUserSummary(user);

        return new AuthResponse(
                jwtToken,
                refreshToken,
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
        // Find session by refresh token hash
        // In production, we'd need to handle the fact that we can't lookup by hash directly without iterating
        // Or we store the token ID in the JWT refresh token claims.
        // For this implementation, we simulate finding the session.
        
        String refreshToken = request.refreshToken();
        
        UserSession session = userSessionRepository.findAll().stream()
                .filter(s -> s.getStatus() == SessionStatus.ACTIVE && passwordEncoder.matches(refreshToken, s.getRefreshTokenHash()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired refresh token"));

        if (session.getExpiresAt().isBefore(OffsetDateTime.now())) {
            session.setStatus(SessionStatus.EXPIRED);
            userSessionRepository.save(session);
            throw new IllegalStateException("Refresh token has expired");
        }

        UserAccount user = session.getUser();
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        
        String newAccessToken = jwtService.generateToken(userDetails);
        String newRefreshToken = jwtService.generateRefreshToken(userDetails);

        // Rotate Refresh Token: Revoke current session and create a new one
        session.setStatus(SessionStatus.REVOKED);
        session.setRevokedAt(OffsetDateTime.now());
        userSessionRepository.save(session);

        saveUserSession(user, newRefreshToken);

        return new AuthResponse(
                newAccessToken,
                newRefreshToken,
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
        UserSession session = new UserSession();
        session.setUser(user);
        session.setRefreshTokenHash(passwordEncoder.encode(refreshToken));
        session.setStatus(SessionStatus.ACTIVE);
        session.setExpiresAt(OffsetDateTime.now().plusDays(7));
        userSessionRepository.save(session);
    }

    private void revokeAllUserSessions(UserAccount user) {
        var validUserSessions = userSessionRepository.findAll().stream()
                .filter(s -> s.getUser().getId().equals(user.getId()) &&
                        (s.getStatus() == SessionStatus.ACTIVE))
                .toList();

        if (validUserSessions.isEmpty()) return;

        validUserSessions.forEach(session -> {
            session.setStatus(SessionStatus.REVOKED);
            session.setRevokedAt(OffsetDateTime.now());
        });
        userSessionRepository.saveAll(validUserSessions);
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
                    String token = UUID.randomUUID().toString();
                    PasswordResetToken resetToken = new PasswordResetToken();
                    resetToken.setUser(user);
                    resetToken.setTokenHash(passwordEncoder.encode(token));
                    resetToken.setExpiresAt(OffsetDateTime.now().plusMinutes(30));
                    passwordResetTokenRepository.save(resetToken);

                    emailService.sendPasswordResetEmail(user.getEmail(), token);
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

        PasswordResetToken token = passwordResetTokenRepository.findAll().stream()
                .filter(t -> t.getUsedAt() == null && passwordEncoder.matches(request.token(), t.getTokenHash()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired token"));

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

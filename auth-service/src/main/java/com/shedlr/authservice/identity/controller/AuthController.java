package com.shedlr.authservice.identity.controller;

import com.shedlr.authservice.identity.dto.request.*;
import com.shedlr.authservice.identity.dto.response.AuthResponse;
import com.shedlr.authservice.identity.dto.response.GenericMessageResponse;
import com.shedlr.authservice.identity.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AuthController exposes authentication endpoints for the microservice.
 * It delegates business logic to AuthService and returns DTO-based responses.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Endpoints for user registration, login, and account recovery")
public class AuthController {

    private final AuthService authService;

    /**
     * Endpoint for user signup.
     *
     * @param request SignupRequest DTO with validation.
     * @return Success message response.
     */
    @PostMapping("/signup")
    @Operation(summary = "Register a new user", description = "Creates a new user account and sends a verification email")
    public ResponseEntity<GenericMessageResponse> signup(
            @Valid @RequestBody SignupRequest request
    ) {
        return ResponseEntity.ok(authService.signup(request));
    }

    /**
     * Endpoint for user login.
     *
     * @param request LoginRequest DTO with credentials.
     * @return AuthResponse with JWT tokens.
     */
    @PostMapping("/login")
    @Operation(summary = "Authenticate user", description = "Returns access and refresh tokens upon successful login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {
        return ResponseEntity.ok(authService.login(request));
    }

    /**
     * Endpoint for email verification.
     */
    @PostMapping("/verify-email")
    @Operation(summary = "Verify user email", description = "Verifies the user account using the token sent via email")
    public ResponseEntity<GenericMessageResponse> verifyEmail(
            @Valid @RequestBody VerifyEmailRequest request
    ) {
        return ResponseEntity.ok(authService.verifyEmail(request));
    }

    /**
     * Endpoint for initiating forgot password flow.
     */
    @PostMapping("/forgot-password")
    @Operation(summary = "Forgot password", description = "Initiates password recovery by sending a reset token to the user's email")
    public ResponseEntity<GenericMessageResponse> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request
    ) {
        return ResponseEntity.ok(authService.forgotPassword(request));
    }

    /**
     * Endpoint for resetting password.
     */
    @PostMapping("/reset-password")
    @Operation(summary = "Reset password", description = "Sets a new password using a valid reset token")
    public ResponseEntity<GenericMessageResponse> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request
    ) {
        return ResponseEntity.ok(authService.resetPassword(request));
    }

    /**
     * Endpoint for refreshing access tokens.
     */
    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token", description = "Returns a new access and refresh token pair")
    public ResponseEntity<AuthResponse> refresh(
            @Valid @RequestBody RefreshTokenRequest request
    ) {
        return ResponseEntity.ok(authService.refreshToken(request));
    }

    /**
     * Endpoint for user logout.
     */
    @PostMapping("/logout")
    @Operation(
            summary = "Logout user",
            description = "Revokes all active sessions for the authenticated user",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<GenericMessageResponse> logout(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body(new GenericMessageResponse("User is not authenticated."));
        }
        authService.logout(userDetails.getUsername());
        return ResponseEntity.ok(new GenericMessageResponse("Logged out successfully. All sessions revoked."));
    }

    /**
     * Endpoint for resending verification email.
     */
    @PostMapping("/resend-verification")
    @Operation(summary = "Resend verification email", description = "Generates and sends a new verification token to the user")
    public ResponseEntity<GenericMessageResponse> resendVerification(
            @Valid @RequestBody ResendVerificationRequest request
    ) {
        return ResponseEntity.ok(authService.resendVerificationEmail(request));
    }
}

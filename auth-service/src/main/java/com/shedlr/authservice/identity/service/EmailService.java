package com.shedlr.authservice.identity.service;

/**
 * EmailService defines the contract for sending transactional emails.
 * In production, this would be implemented using JavaMailSender or a 3rd party API (SendGrid, AWS SES).
 */
public interface EmailService {

    /**
     * Sends a verification email to a new user.
     *
     * @param to User's email address.
     * @param token Verification token.
     */
    void sendVerificationEmail(String to, String token);

    /**
     * Sends a password reset email.
     *
     * @param to User's email address.
     * @param token Password reset token.
     */
    void sendPasswordResetEmail(String to, String token);
}

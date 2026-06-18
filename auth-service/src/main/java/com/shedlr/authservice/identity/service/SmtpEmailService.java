package com.shedlr.authservice.identity.service;

import com.shedlr.authservice.common.config.MailProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * SmtpEmailService provides the production implementation of EmailService using JavaMailSender.
 * Emails are sent asynchronously to avoid blocking the user-facing request threads.
 */
@Service
@Primary
@RequiredArgsConstructor
@Slf4j
public class SmtpEmailService implements EmailService {

    private final JavaMailSender mailSender;
    private final MailProperties mailProperties;

    @Async
    @Override
    public void sendVerificationEmail(String to, String token) {
        log.info("Preparing verification email for: {}", to);
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailProperties.getFrom());
            message.setTo(to);
            message.setSubject("Welcome to Shedlr - Verify Your Email");
            
            String verificationLink = mailProperties.getVerificationUrl() + "?token=" + token;
            message.setText("Thank you for signing up! Please verify your email by clicking the link below:\n\n" + 
                            verificationLink + "\n\n" +
                            "This link will expire in 24 hours.");

            mailSender.send(message);
            log.info("Verification email successfully sent to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send verification email to: {}. Error: {}", to, e.getMessage(), e);
        }
    }

    @Async
    @Override
    public void sendPasswordResetEmail(String to, String token) {
        log.info("Preparing password reset email for: {}", to);
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailProperties.getFrom());
            message.setTo(to);
            message.setSubject("Shedlr - Password Reset Request");

            String resetLink = mailProperties.getPasswordResetUrl() + "?token=" + token;
            message.setText("A password reset was requested for your account. Click the link below to reset your password:\n\n" + 
                            resetLink + "\n\n" +
                            "If you did not request this, please ignore this email.\n" +
                            "This link will expire in 30 minutes.");

            mailSender.send(message);
            log.info("Password reset email successfully sent to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send password reset email to: {}. Error: {}", to, e.getMessage(), e);
        }
    }
}

package com.shedlr.authservice.identity.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * LogEmailService is a placeholder implementation that logs emails instead of sending them.
 * This is useful for development and testing without requiring a real SMTP server.
 * Added @Async to simulate asynchronous production behavior (e.g. using a message queue).
 */
@Service
@Slf4j
public class LogEmailService implements EmailService {

    @Async
    @Override
    public void sendVerificationEmail(String to, String token) {
        log.info("[EMAIL] [ASYNC] Verification mail sent to: {}. Token: {}", to, token);
        // Simulate network delay
        try { Thread.sleep(2000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        log.info("[EMAIL] [ASYNC] Verification mail delivery completed for: {}", to);
    }

    @Async
    @Override
    public void sendPasswordResetEmail(String to, String token) {
        log.info("[EMAIL] [ASYNC] Password reset mail sent to: {}. Token: {}", to, token);
        // Simulate network delay
        try { Thread.sleep(2000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        log.info("[EMAIL] [ASYNC] Password reset mail delivery completed for: {}", to);
    }
}

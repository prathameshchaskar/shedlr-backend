package com.shedlr.authservice.common.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

/**
 * MailProperties maps custom application-level email settings from application.yaml.
 * Validated to ensure fail-fast on missing configuration.
 */
@Configuration
@ConfigurationProperties(prefix = "application.mail")
@Getter
@Setter
@Validated
public class MailProperties {

    /** The 'from' address for outgoing emails. */
    @NotBlank(message = "Outgoing mail 'from' address must be configured")
    private String from;

    /** Base URL for email verification. */
    @NotBlank(message = "Email verification URL must be configured")
    private String verificationUrl;

    /** Base URL for password reset. */
    @NotBlank(message = "Password reset URL must be configured")
    private String passwordResetUrl;
}

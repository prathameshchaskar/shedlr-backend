package com.shedlr.authservice.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * MailProperties maps custom application-level email settings from application.yaml.
 */
@Configuration
@ConfigurationProperties(prefix = "application.mail")
@Getter
@Setter
public class MailProperties {

    /** The 'from' address for outgoing emails. */
    private String from;

    /** Base URL for email verification. */
    private String verificationUrl;

    /** Base URL for password reset. */
    private String passwordResetUrl;
}

package com.shedlr.authservice.common.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * SecurityProperties manages JWT, CORS, and Lockout settings.
 */
@Configuration
@ConfigurationProperties(prefix = "application.security")
@Getter
@Setter
@Validated
public class SecurityProperties {

    private final Jwt jwt = new Jwt();
    private final Lockout lockout = new Lockout();
    private final Cors cors = new Cors();

    @Getter
    @Setter
    public static class Jwt {
        @NotBlank(message = "JWT secret key must be configured")
        private String secretKey;

        @NotNull(message = "JWT access token expiration must be configured")
        @Min(1)
        private Long expiration;

        private final RefreshToken refreshToken = new RefreshToken();

        @Getter
        @Setter
        public static class RefreshToken {
            @NotNull(message = "JWT refresh token expiration must be configured")
            @Min(1)
            private Long expiration;
        }

        public Long getRefreshExpiration() {
            return refreshToken.getExpiration();
        }
    }

    @Getter
    @Setter
    public static class Lockout {
        @NotNull
        @Min(1)
        private Integer maxAttempts = 5;

        @NotNull
        @Min(1)
        private Integer durationMinutes = 15;
    }

    @Getter
    @Setter
    public static class Cors {
        private List<String> allowedOrigins;
    }
}

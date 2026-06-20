package com.shedlr.authservice.common.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * RateLimitProperties manages endpoint-specific rate limiting thresholds.
 */
@Configuration
@ConfigurationProperties(prefix = "application.rate-limit")
@Getter
@Setter
@Validated
public class RateLimitProperties {

    private final LimitConfig login = new LimitConfig(5, Duration.ofMinutes(1));
    private final LimitConfig signup = new LimitConfig(3, Duration.ofHours(1));
    private final LimitConfig resendVerification = new LimitConfig(3, Duration.ofHours(1));
    private final LimitConfig updatePendingEmail = new LimitConfig(3, Duration.ofHours(1));

    @Getter
    @Setter
    public static class LimitConfig {
        @NotNull
        @Min(1)
        private Integer capacity;

        @NotNull
        private Duration period;

        public LimitConfig() {}

        public LimitConfig(Integer capacity, Duration period) {
            this.capacity = capacity;
            this.period = period;
        }
    }
}

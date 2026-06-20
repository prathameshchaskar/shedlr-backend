package com.shedlr.authservice.identity.security;

import com.shedlr.authservice.common.config.RateLimitProperties;
import com.shedlr.authservice.common.exception.dto.ApiErrorResponse;
import com.shedlr.authservice.common.exception.errorcode.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.OffsetDateTime;

/**
 * RateLimitingFilter intercepts requests to auth endpoints and applies rate limiting.
 * It uses the client's IP address as the key for rate limiting.
 */
@Component
@RequiredArgsConstructor
public class RateLimitingFilter extends OncePerRequestFilter {

    private final RateLimitingService rateLimitingService;
    private final RateLimitProperties rateLimitProperties;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        String clientIp = getClientIp(request);

        if (isRateLimited(path, clientIp)) {
            ApiErrorResponse errorResponse = new ApiErrorResponse(
                    HttpStatus.TOO_MANY_REQUESTS.value(),
                    ErrorCode.RATE_LIMIT_EXCEEDED.name(),
                    ErrorCode.RATE_LIMIT_EXCEEDED.getMessage(),
                    path,
                    OffsetDateTime.now()
            );

            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isRateLimited(String path, String clientIp) {
        if (path.startsWith("/api/v1/auth/login")) {
            return !rateLimitingService.tryConsume("rate:login:" + clientIp);
        }
        if (path.startsWith("/api/v1/auth/signup")) {
            RateLimitProperties.LimitConfig config = rateLimitProperties.getSignup();
            return !rateLimitingService.tryConsume("rate:signup:" + clientIp, config.getCapacity(), config.getPeriod());
        }
        if (path.startsWith("/api/v1/auth/resend-verification")) {
            RateLimitProperties.LimitConfig config = rateLimitProperties.getResendVerification();
            return !rateLimitingService.tryConsume("rate:resend:" + clientIp, config.getCapacity(), config.getPeriod());
        }
        if (path.startsWith("/api/v1/auth/update-pending-email")) {
            RateLimitProperties.LimitConfig config = rateLimitProperties.getUpdatePendingEmail();
            return !rateLimitingService.tryConsume("rate:update-pending:" + clientIp, config.getCapacity(), config.getPeriod());
        }
        return false;
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }
}

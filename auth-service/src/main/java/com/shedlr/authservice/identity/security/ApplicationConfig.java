package com.shedlr.authservice.identity.security;

import com.shedlr.authservice.identity.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * ApplicationConfig provides beans related to security and authentication.
 * It configures UserDetailsService, PasswordEncoder, and AuthenticationManager.
 */
@Configuration
@RequiredArgsConstructor
public class ApplicationConfig {

    private final UserAccountRepository repository;

    /**
     * Configures UserDetailsService to load user from the database by email.
     *
     * @return UserDetailsService bean.
     */
    @Bean
    public UserDetailsService userDetailsService() {
        return username -> repository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    /**
     * Configures AuthenticationProvider using DAO pattern.
     *
     * @return AuthenticationProvider bean.
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService());
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    /**
     * Configures AuthenticationManager from standard configuration.
     *
     * @param config AuthenticationConfiguration.
     * @return AuthenticationManager bean.
     * @throws Exception if configuration fails.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Configures Argon2PasswordEncoder for secure password hashing.
     * Argon2 is the winner of the Password Hashing Competition and is resistant to GPU cracking.
     *
     * @return PasswordEncoder bean.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    }
}

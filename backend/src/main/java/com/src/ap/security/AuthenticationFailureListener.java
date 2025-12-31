package com.src.ap.security;

import com.src.ap.entity.User;
import com.src.ap.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuthenticationFailureListener {

    private final UserRepository userRepository;

    @Value("${security.login.max-failed-attempts:5}")
    private int maxFailedAttempts;

    @Value("${security.login.lockout-duration-minutes:15}")
    private int lockoutDurationMinutes;

    @EventListener
    @Transactional
    public void onAuthenticationFailure(AuthenticationFailureBadCredentialsEvent event) {
        String username = (String) event.getAuthentication().getPrincipal();
        String errorMessage = event.getException().getMessage();
        String ipAddress = getClientIpAddress(event.getAuthentication());

        userRepository.findByUsername(username).ifPresent(user -> {
            int attempts = user.getFailedLoginAttempts() != null ? user.getFailedLoginAttempts() : 0;
            attempts++;
            user.setFailedLoginAttempts(attempts);
            user.setLastFailedLoginAt(LocalDateTime.now());

            if (attempts >= maxFailedAttempts) {
                user.setAccountLockedUntil(LocalDateTime.now().plusMinutes(lockoutDurationMinutes));
                log.error("Account locked due to {} failed login attempts - Username: '{}', IP: {}, Locked until: {}",
                        attempts, username, ipAddress, user.getAccountLockedUntil());
            } else {
                log.warn("Failed login attempt #{} - Username: '{}', IP: {}, Reason: {}, Timestamp: {}",
                        attempts, username, ipAddress, errorMessage, LocalDateTime.now());
            }

            userRepository.save(user);
        });

        if (userRepository.findByUsername(username).isEmpty()) {
            log.warn("Failed login attempt for non-existent user - Username: '{}', IP: {}, Timestamp: {}",
                    username, ipAddress, LocalDateTime.now());
        }
    }

    @EventListener
    @Transactional
    public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
        String username = event.getAuthentication().getName();
        String ipAddress = getClientIpAddress(event.getAuthentication());

        userRepository.findByUsername(username).ifPresent(user -> {
            if (user.getFailedLoginAttempts() != null && user.getFailedLoginAttempts() > 0) {
                log.info("Resetting failed login attempts for user: '{}'", username);
                user.setFailedLoginAttempts(0);
                user.setLastFailedLoginAt(null);
                user.setAccountLockedUntil(null);
                userRepository.save(user);
            }
        });

        log.info("Successful login - Username: '{}', IP: {}, Timestamp: {}",
                username, ipAddress, LocalDateTime.now());
    }

    /**
     * Extracts the client IP address from the Authentication object.
     * Uses WebAuthenticationDetails if available (from form login or HTTP Basic),
     * otherwise returns "UNKNOWN".
     *
     * @param authentication the authentication object from the security event
     * @return the client IP address or "UNKNOWN" if not available
     */
    private String getClientIpAddress(org.springframework.security.core.Authentication authentication) {
        if (authentication == null) {
            return "UNKNOWN";
        }

        Object details = authentication.getDetails();
        if (details instanceof WebAuthenticationDetails) {
            WebAuthenticationDetails webDetails = (WebAuthenticationDetails) details;
            return webDetails.getRemoteAddress();
        }

        return "UNKNOWN";
    }
}
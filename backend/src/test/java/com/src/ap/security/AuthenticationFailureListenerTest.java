package com.src.ap.security;

import com.src.ap.entity.User;
import com.src.ap.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Authentication Failure Listener Tests")
class AuthenticationFailureListenerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private Authentication authentication;

    @Mock
    private WebAuthenticationDetails webAuthenticationDetails;

    @InjectMocks
    private AuthenticationFailureListener listener;

    private User testUser;
    private static final String USERNAME = "testuser";
    private static final String IP_ADDRESS = "192.168.1.100";
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCKOUT_DURATION_MINUTES = 15;

    @BeforeEach
    void setUp() {
        // Set configuration values
        ReflectionTestUtils.setField(listener, "maxFailedAttempts", MAX_FAILED_ATTEMPTS);
        ReflectionTestUtils.setField(listener, "lockoutDurationMinutes", LOCKOUT_DURATION_MINUTES);

        // Setup test user
        testUser = User.builder()
                .id(1L)
                .username(USERNAME)
                .passwordHash("$2a$12$hashedPassword")
                .email("test@example.com")
                .failedLoginAttempts(0)
                .build();

        // Setup authentication mock
        when(authentication.getPrincipal()).thenReturn(USERNAME);
        when(authentication.getName()).thenReturn(USERNAME);
        when(authentication.getDetails()).thenReturn(webAuthenticationDetails);
        when(webAuthenticationDetails.getRemoteAddress()).thenReturn(IP_ADDRESS);
    }

    // ========== FAILED AUTHENTICATION TESTS ==========

    @Test
    @DisplayName("Should increment failed login attempts on authentication failure")
    void shouldIncrementFailedLoginAttemptsOnAuthenticationFailure() {
        // Given: User with 0 failed attempts
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(testUser));

        BadCredentialsException exception = new BadCredentialsException("Bad credentials");
        AuthenticationFailureBadCredentialsEvent event =
                new AuthenticationFailureBadCredentialsEvent(authentication, exception);

        // When: Authentication fails
        listener.onAuthenticationFailure(event);

        // Then: Failed attempts should be incremented and saved
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getFailedLoginAttempts()).isEqualTo(1);
        assertThat(savedUser.getLastFailedLoginAt()).isNotNull();
    }

    @Test
    @DisplayName("Should set last failed login timestamp on authentication failure")
    void shouldSetLastFailedLoginTimestampOnAuthenticationFailure() {
        // Given: User with 0 failed attempts
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(testUser));

        BadCredentialsException exception = new BadCredentialsException("Bad credentials");
        AuthenticationFailureBadCredentialsEvent event =
                new AuthenticationFailureBadCredentialsEvent(authentication, exception);

        LocalDateTime beforeTest = LocalDateTime.now().minusSeconds(1);

        // When: Authentication fails
        listener.onAuthenticationFailure(event);

        // Then: Last failed login timestamp should be set
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getLastFailedLoginAt())
                .isNotNull()
                .isAfter(beforeTest)
                .isBefore(LocalDateTime.now().plusSeconds(1));
    }

    @Test
    @DisplayName("Should lock account after max failed attempts")
    void shouldLockAccountAfterMaxFailedAttempts() {
        // Given: User with 4 failed attempts (one more will reach limit)
        testUser.setFailedLoginAttempts(4);
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(testUser));

        BadCredentialsException exception = new BadCredentialsException("Bad credentials");
        AuthenticationFailureBadCredentialsEvent event =
                new AuthenticationFailureBadCredentialsEvent(authentication, exception);

        LocalDateTime beforeTest = LocalDateTime.now();

        // When: Authentication fails (5th attempt)
        listener.onAuthenticationFailure(event);

        // Then: Account should be locked
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getFailedLoginAttempts()).isEqualTo(5);
        assertThat(savedUser.getAccountLockedUntil())
                .isNotNull()
                .isAfter(beforeTest.plusMinutes(LOCKOUT_DURATION_MINUTES - 1))
                .isBefore(beforeTest.plusMinutes(LOCKOUT_DURATION_MINUTES + 1));
    }

    @Test
    @DisplayName("Should not lock account before reaching max attempts")
    void shouldNotLockAccountBeforeReachingMaxAttempts() {
        // Given: User with 3 failed attempts (below limit)
        testUser.setFailedLoginAttempts(3);
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(testUser));

        BadCredentialsException exception = new BadCredentialsException("Bad credentials");
        AuthenticationFailureBadCredentialsEvent event =
                new AuthenticationFailureBadCredentialsEvent(authentication, exception);

        // When: Authentication fails (4th attempt)
        listener.onAuthenticationFailure(event);

        // Then: Account should not be locked yet
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getFailedLoginAttempts()).isEqualTo(4);
        assertThat(savedUser.getAccountLockedUntil()).isNull();
    }

    @Test
    @DisplayName("Should handle authentication failure for non-existent user")
    void shouldHandleAuthenticationFailureForNonExistentUser() {
        // Given: User does not exist
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.empty());

        BadCredentialsException exception = new BadCredentialsException("Bad credentials");
        AuthenticationFailureBadCredentialsEvent event =
                new AuthenticationFailureBadCredentialsEvent(authentication, exception);

        // When: Authentication fails
        listener.onAuthenticationFailure(event);

        // Then: Should not attempt to save any user
        verify(userRepository, never()).save(any(User.class));

        // And: Should still log the attempt (verified by no exception thrown)
        verify(userRepository, times(2)).findByUsername(USERNAME);
    }

    @Test
    @DisplayName("Should extract IP address from authentication details")
    void shouldExtractIpAddressFromAuthenticationDetails() {
        // Given: Authentication with IP address in details
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(testUser));

        BadCredentialsException exception = new BadCredentialsException("Bad credentials");
        AuthenticationFailureBadCredentialsEvent event =
                new AuthenticationFailureBadCredentialsEvent(authentication, exception);

        // When: Authentication fails
        listener.onAuthenticationFailure(event);

        // Then: Should extract IP from WebAuthenticationDetails
        verify(webAuthenticationDetails).getRemoteAddress();
    }

    // ========== SUCCESSFUL AUTHENTICATION TESTS ==========

    @Test
    @DisplayName("Should reset failed login attempts on successful authentication")
    void shouldResetFailedLoginAttemptsOnSuccessfulAuthentication() {
        // Given: User with 3 failed attempts
        testUser.setFailedLoginAttempts(3);
        testUser.setLastFailedLoginAt(LocalDateTime.now().minusMinutes(5));
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(testUser));

        AuthenticationSuccessEvent event = new AuthenticationSuccessEvent(authentication);

        // When: Authentication succeeds
        listener.onAuthenticationSuccess(event);

        // Then: Failed attempts should be reset
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getFailedLoginAttempts()).isEqualTo(0);
        assertThat(savedUser.getLastFailedLoginAt()).isNull();
    }

    @Test
    @DisplayName("Should clear account lock on successful authentication")
    void shouldClearAccountLockOnSuccessfulAuthentication() {
        // Given: User with locked account
        testUser.setFailedLoginAttempts(5);
        testUser.setAccountLockedUntil(LocalDateTime.now().plusMinutes(10));
        testUser.setLastFailedLoginAt(LocalDateTime.now().minusMinutes(5));
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(testUser));

        AuthenticationSuccessEvent event = new AuthenticationSuccessEvent(authentication);

        // When: Authentication succeeds
        listener.onAuthenticationSuccess(event);

        // Then: Account lock should be cleared
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getAccountLockedUntil()).isNull();
        assertThat(savedUser.getFailedLoginAttempts()).isEqualTo(0);
        assertThat(savedUser.getLastFailedLoginAt()).isNull();
    }

    @Test
    @DisplayName("Should not save user when failed attempts is already zero on success")
    void shouldNotSaveUserWhenFailedAttemptsAlreadyZeroOnSuccess() {
        // Given: User with no failed attempts
        testUser.setFailedLoginAttempts(0);
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(testUser));

        AuthenticationSuccessEvent event = new AuthenticationSuccessEvent(authentication);

        // When: Authentication succeeds
        listener.onAuthenticationSuccess(event);

        // Then: User should not be saved (no changes needed)
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should handle successful authentication for non-existent user gracefully")
    void shouldHandleSuccessfulAuthenticationForNonExistentUserGracefully() {
        // Given: User does not exist (edge case - shouldn't happen in practice)
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.empty());

        AuthenticationSuccessEvent event = new AuthenticationSuccessEvent(authentication);

        // When: Authentication succeeds
        listener.onAuthenticationSuccess(event);

        // Then: Should not attempt to save
        verify(userRepository, never()).save(any(User.class));
    }

    // ========== IP ADDRESS EXTRACTION TESTS ==========

    @Test
    @DisplayName("Should return UNKNOWN when authentication details are null")
    void shouldReturnUnknownWhenAuthenticationDetailsAreNull() {
        // Given: Authentication without details
        when(authentication.getDetails()).thenReturn(null);
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(testUser));

        BadCredentialsException exception = new BadCredentialsException("Bad credentials");
        AuthenticationFailureBadCredentialsEvent event =
                new AuthenticationFailureBadCredentialsEvent(authentication, exception);

        // When: Authentication fails
        listener.onAuthenticationFailure(event);

        // Then: Should handle gracefully (no exception)
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should return UNKNOWN when authentication is null")
    void shouldReturnUnknownWhenAuthenticationIsNull() {
        // Given: Null authentication (edge case)
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(testUser));

        // Create event with null authentication details
        Authentication nullDetailsAuth = mock(Authentication.class);
        when(nullDetailsAuth.getPrincipal()).thenReturn(USERNAME);
        when(nullDetailsAuth.getDetails()).thenReturn(null);

        BadCredentialsException exception = new BadCredentialsException("Bad credentials");
        AuthenticationFailureBadCredentialsEvent event =
                new AuthenticationFailureBadCredentialsEvent(nullDetailsAuth, exception);

        // When: Authentication fails
        listener.onAuthenticationFailure(event);

        // Then: Should handle gracefully
        verify(userRepository).save(any(User.class));
    }

    // ========== VERIFICATION TESTS ==========

    @Test
    @DisplayName("Should verify that failed authentication triggers the listener")
    void shouldVerifyThatFailedAuthenticationTriggersListener() {
        // Given: User exists
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(testUser));

        BadCredentialsException exception = new BadCredentialsException("Bad credentials");
        AuthenticationFailureBadCredentialsEvent event =
                new AuthenticationFailureBadCredentialsEvent(authentication, exception);

        // When: Listener is invoked
        listener.onAuthenticationFailure(event);

        // Then: Listener should process the event
        verify(userRepository, times(2)).findByUsername(USERNAME);
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should verify that successful authentication triggers the listener")
    void shouldVerifyThatSuccessfulAuthenticationTriggersListener() {
        // Given: User with failed attempts
        testUser.setFailedLoginAttempts(2);
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(testUser));

        AuthenticationSuccessEvent event = new AuthenticationSuccessEvent(authentication);

        // When: Listener is invoked
        listener.onAuthenticationSuccess(event);

        // Then: Listener should process the event
        verify(userRepository).findByUsername(USERNAME);
        verify(userRepository).save(any(User.class));
    }
}
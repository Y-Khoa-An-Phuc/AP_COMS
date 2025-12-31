package com.src.ap.service;

import com.src.ap.dto.auth.ChangePasswordRequest;
import com.src.ap.entity.User;
import com.src.ap.exception.BadRequestException;
import com.src.ap.exception.ResourceNotFoundException;
import com.src.ap.exception.UnauthorizedException;
import com.src.ap.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AuthService - Password Change Tests")
class AuthServicePasswordChangeTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private static final String USERNAME = "testuser";
    private static final String CURRENT_PASSWORD = "OldP@ssword123";
    private static final String CURRENT_PASSWORD_HASH = "$2a$12$hashedOldPassword";
    private static final String NEW_PASSWORD = "NewSecureP@ss456";
    private static final String NEW_PASSWORD_HASH = "$2a$12$hashedNewPassword";

    @BeforeEach
    void setUp() {
        // Setup test user
        testUser = User.builder()
                .id(1L)
                .username(USERNAME)
                .passwordHash(CURRENT_PASSWORD_HASH)
                .email("test@example.com")
                .build();

        // Setup security context
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(USERNAME);
    }

    // ========== SUCCESS SCENARIOS ==========

    @Test
    @DisplayName("Should successfully change password when all inputs are valid")
    void shouldSuccessfullyChangePasswordWhenInputsAreValid() {
        // Given: Valid password change request
        ChangePasswordRequest request = new ChangePasswordRequest(
                CURRENT_PASSWORD,
                NEW_PASSWORD,
                NEW_PASSWORD // confirmation matches
        );

        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(CURRENT_PASSWORD, CURRENT_PASSWORD_HASH)).thenReturn(true);
        when(passwordEncoder.matches(NEW_PASSWORD, CURRENT_PASSWORD_HASH)).thenReturn(false);
        when(passwordEncoder.encode(NEW_PASSWORD)).thenReturn(NEW_PASSWORD_HASH);

        // When: Changing password
        authService.changePassword(request);

        // Then: Password should be encoded and user should be saved
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getPasswordHash()).isEqualTo(NEW_PASSWORD_HASH);
        assertThat(savedUser.getUsername()).isEqualTo(USERNAME);

        // Verify password encoder was called correctly
        verify(passwordEncoder).matches(CURRENT_PASSWORD, CURRENT_PASSWORD_HASH);
        verify(passwordEncoder).encode(NEW_PASSWORD);
    }

    @Test
    @DisplayName("Should call password encoder with correct strength when changing password")
    void shouldCallPasswordEncoderWithCorrectStrength() {
        // Given: Valid password change request
        ChangePasswordRequest request = new ChangePasswordRequest(
                CURRENT_PASSWORD,
                NEW_PASSWORD,
                NEW_PASSWORD
        );

        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(CURRENT_PASSWORD, CURRENT_PASSWORD_HASH)).thenReturn(true);
        when(passwordEncoder.matches(NEW_PASSWORD, CURRENT_PASSWORD_HASH)).thenReturn(false);
        when(passwordEncoder.encode(NEW_PASSWORD)).thenReturn(NEW_PASSWORD_HASH);

        // When: Changing password
        authService.changePassword(request);

        // Then: Password encoder should be called exactly once for encoding
        verify(passwordEncoder, times(1)).encode(NEW_PASSWORD);
    }

    // ========== FAILURE SCENARIOS - WRONG CURRENT PASSWORD ==========

    @Test
    @DisplayName("Should fail when current password is incorrect")
    void shouldFailWhenCurrentPasswordIsIncorrect() {
        // Given: Request with wrong current password
        ChangePasswordRequest request = new ChangePasswordRequest(
                "WrongPassword123!",
                NEW_PASSWORD,
                NEW_PASSWORD
        );

        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("WrongPassword123!", CURRENT_PASSWORD_HASH)).thenReturn(false);

        // When/Then: Should throw UnauthorizedException
        assertThatThrownBy(() -> authService.changePassword(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Current password is incorrect");

        // Verify no password encoding or saving occurred
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should not encode new password when current password verification fails")
    void shouldNotEncodeNewPasswordWhenCurrentPasswordFails() {
        // Given: Request with incorrect current password
        ChangePasswordRequest request = new ChangePasswordRequest(
                "WrongPassword",
                NEW_PASSWORD,
                NEW_PASSWORD
        );

        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("WrongPassword", CURRENT_PASSWORD_HASH)).thenReturn(false);

        // When: Attempting to change password
        try {
            authService.changePassword(request);
        } catch (UnauthorizedException e) {
            // Expected exception
        }

        // Then: New password should never be encoded
        verify(passwordEncoder, never()).encode(NEW_PASSWORD);
    }

    // ========== FAILURE SCENARIOS - PASSWORD POLICY VIOLATIONS ==========

    @Test
    @DisplayName("Should fail when new password confirmation does not match")
    void shouldFailWhenPasswordConfirmationDoesNotMatch() {
        // Given: Request with mismatched confirmation
        ChangePasswordRequest request = new ChangePasswordRequest(
                CURRENT_PASSWORD,
                NEW_PASSWORD,
                "DifferentP@ss456" // different from newPassword
        );

        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(CURRENT_PASSWORD, CURRENT_PASSWORD_HASH)).thenReturn(true);

        // When/Then: Should throw BadRequestException
        assertThatThrownBy(() -> authService.changePassword(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("New password and confirmation do not match");

        // Verify no password encoding or saving occurred
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should fail when new password is same as current password")
    void shouldFailWhenNewPasswordIsSameAsCurrent() {
        // Given: Request where new password matches current password
        ChangePasswordRequest request = new ChangePasswordRequest(
                CURRENT_PASSWORD,
                CURRENT_PASSWORD, // same as current
                CURRENT_PASSWORD
        );

        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(CURRENT_PASSWORD, CURRENT_PASSWORD_HASH)).thenReturn(true);

        // When/Then: Should throw BadRequestException
        assertThatThrownBy(() -> authService.changePassword(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("New password must be different from current password");

        // Verify no saving occurred
        verify(userRepository, never()).save(any(User.class));
    }

    // ========== FAILURE SCENARIOS - USER NOT FOUND ==========

    @Test
    @DisplayName("Should fail when user is not found")
    void shouldFailWhenUserNotFound() {
        // Given: User does not exist
        ChangePasswordRequest request = new ChangePasswordRequest(
                CURRENT_PASSWORD,
                NEW_PASSWORD,
                NEW_PASSWORD
        );

        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.empty());

        // When/Then: Should throw ResourceNotFoundException
        assertThatThrownBy(() -> authService.changePassword(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User")
                .hasMessageContaining(USERNAME);

        // Verify no password operations occurred
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    // ========== EDGE CASES ==========

    @Test
    @DisplayName("Should retrieve username from security context")
    void shouldRetrieveUsernameFromSecurityContext() {
        // Given: Valid request
        ChangePasswordRequest request = new ChangePasswordRequest(
                CURRENT_PASSWORD,
                NEW_PASSWORD,
                NEW_PASSWORD
        );

        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(CURRENT_PASSWORD, CURRENT_PASSWORD_HASH)).thenReturn(true);
        when(passwordEncoder.matches(NEW_PASSWORD, CURRENT_PASSWORD_HASH)).thenReturn(false);
        when(passwordEncoder.encode(NEW_PASSWORD)).thenReturn(NEW_PASSWORD_HASH);

        // When: Changing password
        authService.changePassword(request);

        // Then: Should have retrieved username from authentication
        verify(authentication).getName();
        verify(userRepository).findByUsername(USERNAME);
    }

    @Test
    @DisplayName("Should verify current password before checking confirmation match")
    void shouldVerifyCurrentPasswordBeforeConfirmationCheck() {
        // Given: Request with wrong current password and mismatched confirmation
        ChangePasswordRequest request = new ChangePasswordRequest(
                "WrongPassword",
                NEW_PASSWORD,
                "DifferentConfirmation"
        );

        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("WrongPassword", CURRENT_PASSWORD_HASH)).thenReturn(false);

        // When/Then: Should throw UnauthorizedException (current password check)
        // not BadRequestException (confirmation check)
        assertThatThrownBy(() -> authService.changePassword(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Current password is incorrect");
    }

    @Test
    @DisplayName("Should persist updated password hash to database")
    void shouldPersistUpdatedPasswordHashToDatabase() {
        // Given: Valid password change request
        ChangePasswordRequest request = new ChangePasswordRequest(
                CURRENT_PASSWORD,
                NEW_PASSWORD,
                NEW_PASSWORD
        );

        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(CURRENT_PASSWORD, CURRENT_PASSWORD_HASH)).thenReturn(true);
        when(passwordEncoder.matches(NEW_PASSWORD, CURRENT_PASSWORD_HASH)).thenReturn(false);
        when(passwordEncoder.encode(NEW_PASSWORD)).thenReturn(NEW_PASSWORD_HASH);

        // When: Changing password
        authService.changePassword(request);

        // Then: Repository save should be called exactly once
        verify(userRepository, times(1)).save(testUser);

        // And: The saved user should have the new password hash
        assertThat(testUser.getPasswordHash()).isEqualTo(NEW_PASSWORD_HASH);
    }
}
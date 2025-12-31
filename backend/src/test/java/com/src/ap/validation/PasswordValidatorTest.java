package com.src.ap.validation;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Password Validator Tests")
class PasswordValidatorTest {

    private PasswordValidator passwordValidator;

    @Mock
    private ConstraintValidatorContext context;

    @Mock
    private ConstraintValidatorContext.ConstraintViolationBuilder violationBuilder;

    @BeforeEach
    void setUp() {
        passwordValidator = new PasswordValidator();

        // Mock the context behavior
        when(context.buildConstraintViolationWithTemplate(anyString()))
                .thenReturn(violationBuilder);
        when(violationBuilder.addConstraintViolation()).thenReturn(context);
    }

    // ========== VALID PASSWORD TESTS ==========

    @Test
    @DisplayName("Should accept valid password with all character types")
    void shouldAcceptValidPasswordWithAllCharacterTypes() {
        // Given: A password that meets all requirements
        String validPassword = "MySecureP@ssw0rd";

        // When: Validating the password
        boolean isValid = passwordValidator.isValid(validPassword, context);

        // Then: Password should be valid
        assertThat(isValid).isTrue();
        verify(context, never()).buildConstraintViolationWithTemplate(anyString());
    }

    @Test
    @DisplayName("Should accept password with exactly 8 characters")
    void shouldAcceptPasswordWithMinimumLength() {
        // Given: A password with exactly 8 characters
        String validPassword = "MyP@ss1!";

        // When: Validating the password
        boolean isValid = passwordValidator.isValid(validPassword, context);

        // Then: Password should be valid
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Should accept password with 3 of 4 character types (no special chars)")
    void shouldAcceptPasswordWith3Of4CharacterTypes() {
        // Given: A password with uppercase, lowercase, and digits (no special chars)
        String validPassword = "MySecurePassw0rd";

        // When: Validating the password
        boolean isValid = passwordValidator.isValid(validPassword, context);

        // Then: Password should be valid (3/4 types is enough)
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Should accept very long password")
    void shouldAcceptVeryLongPassword() {
        // Given: A long password within 128 character limit
        String validPassword = "MySecureP@ssw0rd" + "Xz9!".repeat(20);

        // When: Validating the password
        boolean isValid = passwordValidator.isValid(validPassword, context);

        // Then: Password should be valid
        assertThat(isValid).isTrue();
    }

    // ========== INVALID PASSWORD TESTS ==========

    @Test
    @DisplayName("Should reject null password")
    void shouldRejectNullPassword() {
        // Given: A null password
        String password = null;

        // When: Validating the password
        boolean isValid = passwordValidator.isValid(password, context);

        // Then: Password should be invalid
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should reject empty password")
    void shouldRejectEmptyPassword() {
        // Given: An empty password
        String password = "";

        // When: Validating the password
        boolean isValid = passwordValidator.isValid(password, context);

        // Then: Password should be invalid
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should reject password with leading spaces")
    void shouldRejectPasswordWithLeadingSpaces() {
        // Given: A password with leading spaces
        String password = " MySecureP@ss123";

        // When: Validating the password
        boolean isValid = passwordValidator.isValid(password, context);

        // Then: Password should be invalid
        assertThat(isValid).isFalse();
        verify(context).buildConstraintViolationWithTemplate(
                "Password must not contain leading or trailing spaces"
        );
    }

    @Test
    @DisplayName("Should reject password with trailing spaces")
    void shouldRejectPasswordWithTrailingSpaces() {
        // Given: A password with trailing spaces
        String password = "MySecureP@ss123 ";

        // When: Validating the password
        boolean isValid = passwordValidator.isValid(password, context);

        // Then: Password should be invalid
        assertThat(isValid).isFalse();
        verify(context).buildConstraintViolationWithTemplate(
                "Password must not contain leading or trailing spaces"
        );
    }

    @Test
    @DisplayName("Should reject password shorter than 8 characters")
    void shouldRejectTooShortPassword() {
        // Given: A password with only 7 characters
        String password = "Sh0rt!@";

        // When: Validating the password
        boolean isValid = passwordValidator.isValid(password, context);

        // Then: Password should be invalid
        assertThat(isValid).isFalse();
        verify(context).buildConstraintViolationWithTemplate(
                "Password must be at least 8 characters long"
        );
    }

    @Test
    @DisplayName("Should reject password longer than 128 characters")
    void shouldRejectTooLongPassword() {
        // Given: A password exceeding 128 characters
        String password = "MySecureP@ss123" + "a".repeat(120);

        // When: Validating the password
        boolean isValid = passwordValidator.isValid(password, context);

        // Then: Password should be invalid
        assertThat(isValid).isFalse();
        verify(context).buildConstraintViolationWithTemplate(
                "Password must not exceed 128 characters"
        );
    }

    @Test
    @DisplayName("Should reject common password 'password'")
    void shouldRejectCommonPassword() {
        // Given: A password matching exactly a common password from the blacklist
        // "password" is 8 characters (meets min length) but is in common password list
        String password = "password";

        // When: Validating the password
        boolean isValid = passwordValidator.isValid(password, context);

        // Then: Password should be invalid (rejected as common password)
        assertThat(isValid).isFalse();
        verify(context).buildConstraintViolationWithTemplate(
                "Password is too common. Please choose a more secure password"
        );
    }

    @Test
    @DisplayName("Should reject password with insufficient complexity (only lowercase)")
    void shouldRejectPasswordWithOnlyLowercase() {
        // Given: A password with only lowercase letters
        String password = "mypasswordonly";

        // When: Validating the password
        boolean isValid = passwordValidator.isValid(password, context);

        // Then: Password should be invalid (complexity score = 1, needs 3)
        assertThat(isValid).isFalse();
        verify(context).buildConstraintViolationWithTemplate(
                "Password must contain at least 3 of the following: uppercase letters, " +
                        "lowercase letters, digits, special characters"
        );
    }

    @Test
    @DisplayName("Should reject password with only 2 character types")
    void shouldRejectPasswordWithTwoCharacterTypes() {
        // Given: A password with only uppercase and lowercase (no digits or special)
        String password = "MyPasswordOnly";

        // When: Validating the password
        boolean isValid = passwordValidator.isValid(password, context);

        // Then: Password should be invalid (complexity score = 2, needs 3)
        assertThat(isValid).isFalse();
        verify(context).buildConstraintViolationWithTemplate(
                "Password must contain at least 3 of the following: uppercase letters, " +
                        "lowercase letters, digits, special characters"
        );
    }

    // ========== EDGE CASE TESTS ==========

    @Test
    @DisplayName("Should accept password without uppercase (if has 3 other types)")
    void shouldAcceptPasswordWithoutUppercase() {
        // Given: A password with lowercase, digits, and special chars (no uppercase)
        String password = "myp@ssw0rd9!#$";

        // When: Validating the password
        boolean isValid = passwordValidator.isValid(password, context);

        // Then: Password should be valid (3/4 types)
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Should accept password with spaces in the middle")
    void shouldAcceptPasswordWithSpacesInMiddle() {
        // Given: A password with spaces in the middle (but not leading/trailing)
        String password = "My Secure P@ss w0rd";

        // When: Validating the password
        boolean isValid = passwordValidator.isValid(password, context);

        // Then: Password should be valid (spaces in middle are allowed)
        assertThat(isValid).isTrue();
    }
}
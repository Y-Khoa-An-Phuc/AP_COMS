package com.src.ap.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class PasswordValidator implements ConstraintValidator<ValidPassword, String> {

    private static final int MIN_LENGTH = 8;
    private static final int MAX_LENGTH = 128;

    private static final Pattern UPPERCASE_PATTERN = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile("[a-z]");
    private static final Pattern DIGIT_PATTERN = Pattern.compile("\\d");
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile("[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]");

    private static final List<String> COMMON_PASSWORDS = Arrays.asList(
            "password", "Password1", "123456", "12345678", "qwerty",
            "abc123", "password123", "admin", "letmein", "welcome",
            "monkey", "1234567890", "password1", "123456789", "welcome123"
    );

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null || password.isEmpty()) {
            return false;
        }

        context.disableDefaultConstraintViolation();

        if (!password.equals(password.trim())) {
            context.buildConstraintViolationWithTemplate(
                    "Password must not contain leading or trailing spaces"
            ).addConstraintViolation();
            return false;
        }

        if (password.length() < MIN_LENGTH) {
            context.buildConstraintViolationWithTemplate(
                    "Password must be at least " + MIN_LENGTH + " characters long"
            ).addConstraintViolation();
            return false;
        }

        if (password.length() > MAX_LENGTH) {
            context.buildConstraintViolationWithTemplate(
                    "Password must not exceed " + MAX_LENGTH + " characters"
            ).addConstraintViolation();
            return false;
        }

        if (isCommonPassword(password)) {
            context.buildConstraintViolationWithTemplate(
                    "Password is too common. Please choose a more secure password"
            ).addConstraintViolation();
            return false;
        }

        int complexityScore = calculateComplexityScore(password);
        if (complexityScore < 3) {
            context.buildConstraintViolationWithTemplate(
                    "Password must contain at least 3 of the following: uppercase letters, " +
                            "lowercase letters, digits, special characters"
            ).addConstraintViolation();
            return false;
        }

        return true;
    }

    private boolean isCommonPassword(String password) {
        return COMMON_PASSWORDS.stream()
                .anyMatch(common -> password.equalsIgnoreCase(common));
    }

    private int calculateComplexityScore(String password) {
        int score = 0;

        if (UPPERCASE_PATTERN.matcher(password).find()) {
            score++;
        }
        if (LOWERCASE_PATTERN.matcher(password).find()) {
            score++;
        }
        if (DIGIT_PATTERN.matcher(password).find()) {
            score++;
        }
        if (SPECIAL_CHAR_PATTERN.matcher(password).find()) {
            score++;
        }

        return score;
    }
}
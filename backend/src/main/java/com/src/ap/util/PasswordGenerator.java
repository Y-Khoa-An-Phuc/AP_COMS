package com.src.ap.util;

import java.security.SecureRandom;

/**
 * Utility class for generating secure temporary passwords.
 */
public class PasswordGenerator {

    private static final String UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWERCASE = "abcdefghijklmnopqrstuvwxyz";
    private static final String DIGITS = "0123456789";
    private static final String SPECIAL_CHARS = "!@#$%^&*()_+-=[]{}";

    private static final String ALL_CHARS = UPPERCASE + LOWERCASE + DIGITS + SPECIAL_CHARS;
    private static final int DEFAULT_PASSWORD_LENGTH = 12;

    private static final SecureRandom random = new SecureRandom();

    private PasswordGenerator() {
        // Private constructor to prevent instantiation
    }

    /**
     * Generates a random temporary password that meets security requirements.
     * The password will contain:
     * - At least one uppercase letter
     * - At least one lowercase letter
     * - At least one digit
     * - At least one special character
     * - Total length of 12 characters
     *
     * @return a secure temporary password
     */
    public static String generateTemporaryPassword() {
        return generateTemporaryPassword(DEFAULT_PASSWORD_LENGTH);
    }

    /**
     * Generates a random temporary password of specified length.
     * The password will contain at least one character from each category:
     * uppercase, lowercase, digit, and special character.
     *
     * @param length the desired password length (minimum 8)
     * @return a secure temporary password
     * @throws IllegalArgumentException if length is less than 8
     */
    public static String generateTemporaryPassword(int length) {
        if (length < 8) {
            throw new IllegalArgumentException("Password length must be at least 8 characters");
        }

        StringBuilder password = new StringBuilder(length);

        // Ensure at least one character from each category
        password.append(UPPERCASE.charAt(random.nextInt(UPPERCASE.length())));
        password.append(LOWERCASE.charAt(random.nextInt(LOWERCASE.length())));
        password.append(DIGITS.charAt(random.nextInt(DIGITS.length())));
        password.append(SPECIAL_CHARS.charAt(random.nextInt(SPECIAL_CHARS.length())));

        // Fill the rest with random characters from all categories
        for (int i = 4; i < length; i++) {
            password.append(ALL_CHARS.charAt(random.nextInt(ALL_CHARS.length())));
        }

        // Shuffle the password to avoid predictable patterns
        return shuffleString(password.toString());
    }

    /**
     * Shuffles the characters in a string randomly.
     *
     * @param input the string to shuffle
     * @return the shuffled string
     */
    private static String shuffleString(String input) {
        char[] characters = input.toCharArray();
        for (int i = characters.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char temp = characters[i];
            characters[i] = characters[j];
            characters[j] = temp;
        }
        return new String(characters);
    }
}

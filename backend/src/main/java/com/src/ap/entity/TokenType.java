package com.src.ap.entity;

/**
 * Enum representing different types of one-time tokens.
 * These tokens are used for various user actions that require verification.
 */
public enum TokenType {
    /**
     * Token used for first-time login and password setup.
     * Sent to new users via email to set their initial password.
     */
    FIRST_LOGIN,

    /**
     * Token for password reset flow (future use).
     * Can be used when users forget their password.
     */
    PASSWORD_RESET
}

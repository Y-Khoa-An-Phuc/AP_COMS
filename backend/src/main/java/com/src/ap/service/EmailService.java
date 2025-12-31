package com.src.ap.service;

import com.src.ap.entity.User;

/**
 * Service interface for sending emails.
 * Implementations should handle email composition and delivery.
 */
public interface EmailService {

    /**
     * Sends a first-login email to a newly created user.
     * The email contains a link to set up their password.
     *
     * @param user the user to send the email to
     * @param firstLoginLink the complete URL link for first-time password setup
     */
    void sendFirstLoginEmail(User user, String firstLoginLink);

    /**
     * Sends a password reset email to a user.
     * The email contains a link to reset their password.
     *
     * @param user the user to send the email to
     * @param resetLink the complete URL link for password reset
     */
    void sendPasswordResetEmail(User user, String resetLink);

    /**
     * Sends a general notification email to a user.
     *
     * @param to the recipient email address
     * @param subject the email subject
     * @param body the email body (can be HTML)
     */
    void sendEmail(String to, String subject, String body);
}

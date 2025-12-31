package com.src.ap.service.impl;

import com.src.ap.entity.User;
import com.src.ap.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * Stub implementation of EmailService that logs emails instead of sending them.
 * This is useful for development/testing or when SMTP is not configured.
 *
 * Active when the 'smtp' profile is NOT active (default profile).
 * To enable real email sending, activate the 'smtp' profile and configure SMTP credentials.
 */
@Profile("!smtp")
@Service
@Slf4j
public class LoggingEmailService implements EmailService {

    @Override
    public void sendFirstLoginEmail(User user, String firstLoginLink) {
        log.info("=".repeat(80));
        log.info("FIRST LOGIN EMAIL (not actually sent - using LoggingEmailService)");
        log.info("=".repeat(80));
        log.info("To: {} <{}>", user.getUsername(), user.getEmail());
        log.info("Subject: Welcome! Set up your password");
        log.info("");
        log.info("Email Body:");
        log.info("---");
        log.info("Hello {},", user.getUsername());
        log.info("");
        log.info("Your account has been created. Please set up your password by clicking the link below:");
        log.info("");
        log.info("  {}", firstLoginLink);
        log.info("");
        log.info("This link is for one-time use only and does not expire.");
        log.info("");
        log.info("If you did not request this account, please contact your administrator.");
        log.info("");
        log.info("Best regards,");
        log.info("The Team");
        log.info("---");
        log.info("=".repeat(80));
        log.info("");

        // TODO: Replace this with actual email sending when SMTP is configured
        // Example with JavaMailSender:
        // MimeMessage message = mailSender.createMimeMessage();
        // MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        // helper.setTo(user.getEmail());
        // helper.setSubject("Welcome! Set up your password");
        // helper.setText(emailBody, true);
        // mailSender.send(message);
    }

    @Override
    public void sendPasswordResetEmail(User user, String resetLink) {
        log.info("=".repeat(80));
        log.info("PASSWORD RESET EMAIL (not actually sent - using LoggingEmailService)");
        log.info("=".repeat(80));
        log.info("To: {} <{}>", user.getUsername(), user.getEmail());
        log.info("Subject: Password Reset Request");
        log.info("");
        log.info("Email Body:");
        log.info("---");
        log.info("Hello {},", user.getUsername());
        log.info("");
        log.info("You have requested to reset your password. Click the link below:");
        log.info("");
        log.info("  {}", resetLink);
        log.info("");
        log.info("This link is for one-time use only.");
        log.info("");
        log.info("If you did not request this, please ignore this email.");
        log.info("");
        log.info("Best regards,");
        log.info("The Team");
        log.info("---");
        log.info("=".repeat(80));
        log.info("");
    }

    @Override
    public void sendEmail(String to, String subject, String body) {
        log.info("=".repeat(80));
        log.info("EMAIL (not actually sent - using LoggingEmailService)");
        log.info("=".repeat(80));
        log.info("To: {}", to);
        log.info("Subject: {}", subject);
        log.info("");
        log.info("Body:");
        log.info("---");
        log.info("{}", body);
        log.info("---");
        log.info("=".repeat(80));
        log.info("");
    }
}

package com.src.ap.service.impl;

import com.src.ap.config.EmailConfig;
import com.src.ap.entity.User;
import com.src.ap.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

/**
 * SMTP implementation of EmailService using JavaMailSender and Thymeleaf templates.
 * Sends real HTML emails via Gmail SMTP.
 *
 * Activated when 'smtp' profile is active.
 * Use @Primary to override LoggingEmailService when both are available.
 */
@Service
@Primary
@Profile("smtp")
@RequiredArgsConstructor
@Slf4j
public class SmtpEmailService implements EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final EmailConfig emailConfig;

    @Override
    public void sendFirstLoginEmail(User user, String firstLoginLink) {
        try {
            log.info("Preparing to send first-login email to {}", user.getEmail());

            // Prepare template context
            Context context = new Context();
            context.setVariable("username", user.getUsername());
            context.setVariable("email", user.getEmail());
            context.setVariable("firstLoginLink", firstLoginLink);
            context.setVariable("emailTitle", "Chào mừng - Thiết lập Mật khẩu");
            context.setVariable("companyName", emailConfig.getBranding().getCompanyName());
            context.setVariable("supportEmail", emailConfig.getBranding().getSupportEmail());

            // Render email with complete template
            String emailHtml = templateEngine.process("email/first-login-complete", context);

            // Create and send email
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(emailConfig.getFrom().getAddress(), emailConfig.getFrom().getName());
            helper.setTo(user.getEmail());
            helper.setSubject("Chào mừng! Thiết lập Mật khẩu - " + emailConfig.getBranding().getCompanyName());
            helper.setText(emailHtml, true); // true = HTML

            mailSender.send(message);

            log.info("First-login email successfully sent to {} ({})", user.getUsername(), user.getEmail());

        } catch (MessagingException e) {
            log.error("Failed to send first-login email to {} ({})", user.getUsername(), user.getEmail(), e);
            throw new RuntimeException("Failed to send first-login email. Please contact support.", e);
        } catch (Exception e) {
            log.error("Unexpected error sending first-login email to {} ({})", user.getUsername(), user.getEmail(), e);
            throw new RuntimeException("Failed to send email due to unexpected error", e);
        }
    }

    @Override
    public void sendPasswordResetEmail(User user, String resetLink) {
        try {
            log.info("Preparing to send password-reset email to {}", user.getEmail());

            // Prepare template context
            Context context = new Context();
            context.setVariable("username", user.getUsername());
            context.setVariable("email", user.getEmail());
            context.setVariable("resetLink", resetLink);
            context.setVariable("emailTitle", "Yêu cầu Đặt lại Mật khẩu");
            context.setVariable("companyName", emailConfig.getBranding().getCompanyName());
            context.setVariable("supportEmail", emailConfig.getBranding().getSupportEmail());

            // Render email with complete template
            String emailHtml = templateEngine.process("email/password-reset-complete", context);

            // Create and send email
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(emailConfig.getFrom().getAddress(), emailConfig.getFrom().getName());
            helper.setTo(user.getEmail());
            helper.setSubject("Yêu cầu Đặt lại Mật khẩu - " + emailConfig.getBranding().getCompanyName());
            helper.setText(emailHtml, true);

            mailSender.send(message);

            log.info("Password-reset email successfully sent to {} ({})", user.getUsername(), user.getEmail());

        } catch (MessagingException e) {
            log.error("Failed to send password-reset email to {} ({})", user.getUsername(), user.getEmail(), e);
            throw new RuntimeException("Failed to send password-reset email. Please contact support.", e);
        } catch (Exception e) {
            log.error("Unexpected error sending password-reset email to {} ({})", user.getUsername(), user.getEmail(), e);
            throw new RuntimeException("Failed to send email due to unexpected error", e);
        }
    }

    @Override
    public void sendEmail(String to, String subject, String body) {
        try {
            log.info("Sending general email to {}", to);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(emailConfig.getFrom().getAddress(), emailConfig.getFrom().getName());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, true); // Assume HTML

            mailSender.send(message);

            log.info("General email successfully sent to {}", to);

        } catch (MessagingException e) {
            log.error("Failed to send email to {}", to, e);
            throw new RuntimeException("Failed to send email", e);
        } catch (Exception e) {
            log.error("Unexpected error sending email to {}", to, e);
            throw new RuntimeException("Failed to send email due to unexpected error", e);
        }
    }
}

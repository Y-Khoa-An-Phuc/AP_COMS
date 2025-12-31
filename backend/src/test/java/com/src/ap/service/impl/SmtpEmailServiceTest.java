package com.src.ap.service.impl;

import com.src.ap.config.EmailConfig;
import com.src.ap.entity.User;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SmtpEmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private TemplateEngine templateEngine;

    @Mock
    private EmailConfig emailConfig;

    private SmtpEmailService emailService;

    private User testUser;

    @BeforeEach
    void setUp() {
        // Configure EmailConfig mock
        EmailConfig.From from = new EmailConfig.From();
        from.setAddress("noreply@anphuc.com");
        from.setName("An Phuc Test");

        EmailConfig.Branding branding = new EmailConfig.Branding();
        branding.setCompanyName("An Phuc Contract Management");
        branding.setSupportEmail("support@anphuc.com");

        when(emailConfig.getFrom()).thenReturn(from);
        when(emailConfig.getBranding()).thenReturn(branding);

        // Create service instance
        emailService = new SmtpEmailService(mailSender, templateEngine, emailConfig);

        // Create test user
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .build();

        // Mock MimeMessage creation
        MimeMessage mimeMessage = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
    }

    @Test
    void sendFirstLoginEmail_Success() {
        // Given
        String firstLoginLink = "http://localhost:4200/first-login?token=abc123";
        when(templateEngine.process(eq("email/first-login-complete"), any(Context.class)))
                .thenReturn("<html>Rendered email</html>");

        // When
        emailService.sendFirstLoginEmail(testUser, firstLoginLink);

        // Then
        verify(mailSender, times(1)).send(any(MimeMessage.class));
        verify(templateEngine, times(1)).process(eq("email/first-login-complete"), any(Context.class));
    }

    @Test
    void sendFirstLoginEmail_TemplateContextContainsCorrectVariables() {
        // Given
        String firstLoginLink = "http://localhost:4200/first-login?token=abc123";
        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        when(templateEngine.process(eq("email/first-login-complete"), contextCaptor.capture()))
                .thenReturn("<html>Rendered email</html>");

        // When
        emailService.sendFirstLoginEmail(testUser, firstLoginLink);

        // Then
        Context capturedContext = contextCaptor.getValue();
        assertThat(capturedContext.getVariable("username")).isEqualTo("testuser");
        assertThat(capturedContext.getVariable("email")).isEqualTo("test@example.com");
        assertThat(capturedContext.getVariable("firstLoginLink")).isEqualTo(firstLoginLink);
        assertThat(capturedContext.getVariable("emailTitle")).isEqualTo("Chào mừng - Thiết lập Mật khẩu");
    }

    @Test
    void sendPasswordResetEmail_Success() {
        // Given
        String resetLink = "http://localhost:4200/reset-password?token=xyz789";
        when(templateEngine.process(eq("email/password-reset-complete"), any(Context.class)))
                .thenReturn("<html>Rendered email</html>");

        // When
        emailService.sendPasswordResetEmail(testUser, resetLink);

        // Then
        verify(mailSender, times(1)).send(any(MimeMessage.class));
        verify(templateEngine, times(1)).process(eq("email/password-reset-complete"), any(Context.class));
    }

    @Test
    void sendPasswordResetEmail_TemplateContextContainsCorrectVariables() {
        // Given
        String resetLink = "http://localhost:4200/reset-password?token=xyz789";
        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        when(templateEngine.process(eq("email/password-reset-complete"), contextCaptor.capture()))
                .thenReturn("<html>Rendered email</html>");

        // When
        emailService.sendPasswordResetEmail(testUser, resetLink);

        // Then
        Context capturedContext = contextCaptor.getValue();
        assertThat(capturedContext.getVariable("username")).isEqualTo("testuser");
        assertThat(capturedContext.getVariable("email")).isEqualTo("test@example.com");
        assertThat(capturedContext.getVariable("resetLink")).isEqualTo(resetLink);
        assertThat(capturedContext.getVariable("emailTitle")).isEqualTo("Yêu cầu Đặt lại Mật khẩu");
    }

    @Test
    void sendEmail_Success() {
        // Given
        String to = "recipient@example.com";
        String subject = "Test Subject";
        String body = "<html>Test body</html>";

        // When
        emailService.sendEmail(to, subject, body);

        // Then
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    void sendFirstLoginEmail_ThrowsRuntimeException_WhenMailSenderFails() {
        // Given
        String firstLoginLink = "http://localhost:4200/first-login?token=abc123";
        when(templateEngine.process(eq("email/first-login-complete"), any(Context.class)))
                .thenReturn("<html>Rendered email</html>");
        doThrow(new RuntimeException("SMTP connection failed"))
                .when(mailSender).send(any(MimeMessage.class));

        // When & Then
        assertThatThrownBy(() -> emailService.sendFirstLoginEmail(testUser, firstLoginLink))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to send email");
    }

    @Test
    void sendPasswordResetEmail_ThrowsRuntimeException_WhenTemplateEngineFails() {
        // Given
        String resetLink = "http://localhost:4200/reset-password?token=xyz789";
        when(templateEngine.process(eq("email/password-reset-complete"), any(Context.class)))
                .thenThrow(new RuntimeException("Template rendering failed"));

        // When & Then
        assertThatThrownBy(() -> emailService.sendPasswordResetEmail(testUser, resetLink))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to send email");
    }
}

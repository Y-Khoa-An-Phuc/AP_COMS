# Gmail SMTP Setup Guide

## Prerequisites
- A Gmail account
- Two-Factor Authentication (2FA) enabled on your Gmail account

## Steps to Generate Gmail App Password

### 1. Enable Two-Factor Authentication (2FA)
If not already enabled:
1. Go to [Google Account Security](https://myaccount.google.com/security)
2. Under "Signing in to Google", select "2-Step Verification"
3. Follow the setup wizard to enable 2FA

### 2. Generate App Password
1. Go to [Google Account Security](https://myaccount.google.com/security)
2. Under "Signing in to Google", select "App passwords"
   - If you don't see this option, ensure 2FA is enabled
3. Click "Select app" → Choose "Mail"
4. Click "Select device" → Choose "Other (Custom name)"
5. Enter a name: "An Phuc Backend SMTP"
6. Click "Generate"
7. Google will display a 16-character password (e.g., `abcd efgh ijkl mnop`)
8. **Copy this password immediately** (you won't be able to see it again)

### 3. Configure Application
Update your `.env` file:

```properties
SMTP_USERNAME=your-gmail@gmail.com
SMTP_APP_PASSWORD=abcdefghijklmnop  # Remove spaces from the generated password
SMTP_FROM_ADDRESS=your-gmail@gmail.com
SMTP_FROM_NAME=An Phuc Contract Management
SMTP_SUPPORT_EMAIL=your-gmail@gmail.com
```

**Important**: Remove all spaces from the app password!

### 4. Activate SMTP Profile
Set the Spring profile to enable SMTP:

```properties
# In .env file
SPRING_PROFILES_ACTIVE=smtp
```

Or when running the application:
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=smtp
```

### 5. Test Email Sending
1. Start the application with the smtp profile
2. Register a new user or trigger the DataSeeder
3. Check the recipient's inbox for the email
4. Check application logs for confirmation:
   ```
   First-login email successfully sent to username (email@example.com)
   ```

## Troubleshooting

### Issue: "Username and Password not accepted"
**Solution**:
- Verify 2FA is enabled on your Gmail account
- Regenerate the App Password
- Ensure no spaces in SMTP_APP_PASSWORD
- Double-check SMTP_USERNAME matches the Gmail address

### Issue: "Connection timed out"
**Solution**:
- Check firewall settings (allow port 587)
- Verify internet connection
- Try using port 465 with SSL instead:
  ```yaml
  spring:
    mail:
      port: 465
      properties:
        mail:
          smtp:
            ssl:
              enable: true
  ```

### Issue: Emails go to Spam
**Solution**:
- Add your email to the recipient's contacts
- For production, use a custom domain with SPF/DKIM records
- Use a professional email service (SendGrid, AWS SES) for production

### Issue: "Less secure app access"
**Note**: Gmail deprecated "Less secure apps" in May 2022. You MUST use App Passwords with 2FA.

## Gmail Sending Limits
- **Free Gmail**: 500 emails per day
- **Google Workspace**: 2,000 emails per day

For high-volume production use, consider:
- SendGrid (100 emails/day free, scalable)
- AWS SES (pay-as-you-go, cheaper at scale)
- Mailgun
- Postmark

## Security Best Practices
1. **Never commit** `.env` to version control
2. Use **environment variables** in production (Kubernetes secrets, etc.)
3. **Rotate App Passwords** periodically
4. **Monitor** email sending logs for suspicious activity
5. **Limit access** to SMTP credentials (principle of least privilege)

## Development vs Production

### Development (LoggingEmailService)
```bash
# No SMTP configuration needed
# Set SPRING_PROFILES_ACTIVE=default in .env
mvn spring-boot:run
# Emails are logged to console
```

### Production (SmtpEmailService)
```bash
# Set environment variables in .env
SPRING_PROFILES_ACTIVE=smtp
SMTP_USERNAME=your-gmail@gmail.com
SMTP_APP_PASSWORD=your-app-password

# Run application
mvn spring-boot:run
# Real emails are sent via Gmail SMTP
```

## Testing Checklist
- [ ] 2FA enabled on Gmail account
- [ ] App Password generated
- [ ] `.env` file updated with correct credentials
- [ ] SPRING_PROFILES_ACTIVE=smtp set
- [ ] Application starts without errors
- [ ] Test email sent successfully
- [ ] Email received in inbox (check spam folder too)
- [ ] Email displays correctly (HTML formatting)
- [ ] Links in email work correctly

## Email Templates

The application uses Thymeleaf templates for professional HTML emails:

- **base.html**: Base template with responsive design and company branding
- **first-login.html**: Welcome email for new users to set up their password
- **password-reset.html**: Password reset request email

Templates are located in: `src/main/resources/templates/email/`

## How It Works

The application uses Spring Profiles to switch between email implementations:

1. **Default Profile** (`SPRING_PROFILES_ACTIVE=default`):
   - Uses `LoggingEmailService`
   - Emails are logged to console
   - No SMTP configuration required
   - Ideal for development and testing

2. **SMTP Profile** (`SPRING_PROFILES_ACTIVE=smtp`):
   - Uses `SmtpEmailService` (marked with `@Primary`)
   - Real emails sent via Gmail SMTP
   - Requires SMTP credentials in `.env`
   - Uses professional HTML templates

## Support

If you encounter issues:
1. Check the application logs for detailed error messages
2. Verify your Gmail App Password is correct
3. Ensure 2FA is enabled on your Gmail account
4. Check that port 587 is not blocked by firewall
5. Review the troubleshooting section above

For additional help, contact your system administrator or refer to the Spring Mail documentation: https://docs.spring.io/spring-boot/docs/current/reference/html/io.html#io.email

# Email Integration for First Login Flow

## Overview
Implemented email service to send first-login setup links to newly created users. The system uses a configurable frontend base URL and sends emails via an `EmailService` interface.

---

## Components Implemented

### 1. EmailService Interface
**File:** `src/main/java/com/src/ap/service/EmailService.java`

```java
public interface EmailService {
    void sendFirstLoginEmail(User user, String firstLoginLink);
    void sendPasswordResetEmail(User user, String resetLink);
    void sendEmail(String to, String subject, String body);
}
```

**Purpose:** Abstract interface for email sending, allowing different implementations (logging, SMTP, SendGrid, etc.)

---

### 2. LoggingEmailService (Stub Implementation)
**File:** `src/main/java/com/src/ap/service/impl/LoggingEmailService.java`

**Current Behavior:** Logs emails to console instead of sending them

**Example Output:**
```
================================================================================
FIRST LOGIN EMAIL (not actually sent - using LoggingEmailService)
================================================================================
To: johndoe <john@example.com>
Subject: Welcome! Set up your password

Email Body:
---
Hello johndoe,

Your account has been created. Please set up your password by clicking the link below:

  http://localhost:3000/first-login?token=a1B2c3D4e5F6g7H8i9J0k1L2m3N4o5P6q7R8s9T0u1V2

This link is for one-time use only and does not expire.

If you did not request this account, please contact your administrator.

Best regards,
The Team
---
================================================================================
```

**Why a stub?**
- No SMTP configuration required to run the app
- Perfect for development and testing
- Easy to see what emails would be sent in the logs
- Can be replaced with real implementation when ready

---

### 3. Frontend Base URL Configuration
**File:** `src/main/resources/application.yml`

```yaml
app:
  frontend:
    base-url: ${FRONTEND_BASE_URL:http://localhost:3000}
```

**Environment Variable:** `FRONTEND_BASE_URL`
**Default Value:** `http://localhost:3000`

**Examples:**
- Development: `http://localhost:3000`
- Staging: `https://staging.yourapp.com`
- Production: `https://yourapp.com`

---

## Integration Points

### 1. User Registration (TechAdmin Creates User)
**Location:** `AuthService.register()`

```java
@Transactional
public AuthResponse register(RegisterRequest request) {
    // ... validation ...

    // Create user with temporary password flags
    User user = User.builder()
            .username(request.getUsername())
            .passwordHash(passwordEncoder.encode(request.getPassword()))
            .email(request.getEmail())
            .enabled(true)
            .mustChangePassword(true)      // ✅ Set
            .temporaryPassword(true)        // ✅ Set
            .build();

    userRepository.save(user);              // ✅ Save user

    // Generate one-time token
    OneTimeToken firstLoginToken = oneTimeTokenService.createFirstLoginToken(user);

    // Compose email link
    String firstLoginLink = frontendBaseUrl + "/first-login?token=" + firstLoginToken.getToken();

    // Send email
    emailService.sendFirstLoginEmail(user, firstLoginLink);

    // ... return response ...
}
```

**Flow:**
1. ✅ User saved with `temporaryPassword=true` and `mustChangePassword=true`
2. ✅ OneTimeToken created immediately after saving
3. ✅ Email link composed using configurable `frontendBaseUrl`
4. ✅ Email sent using `EmailService`

---

### 2. Initial Admin Creation (DataSeeder)
**Location:** `DataSeeder.seedSuperAdminUser()`

```java
private void seedSuperAdminUser() {
    // ... create admin user ...

    userRepository.save(superAdmin);

    // Generate one-time token
    OneTimeToken firstLoginToken = oneTimeTokenService.createFirstLoginToken(superAdmin);

    // Compose email link
    String firstLoginLink = frontendBaseUrl + "/first-login?token=" + firstLoginToken.getToken();

    // Send email
    emailService.sendFirstLoginEmail(superAdmin, firstLoginLink);

    log.info("Created initial super Admin user: {}", superAdminUsername);
    log.info("First login email sent to: {}", superAdminEmail);
}
```

---

## Email Content

### First Login Email

**To:** User's email address
**Subject:** Welcome! Set up your password

**Body:**
```
Hello {username},

Your account has been created. Please set up your password by clicking the link below:

  {firstLoginLink}

This link is for one-time use only and does not expire.

If you did not request this account, please contact your administrator.

Best regards,
The Team
```

**Link Format:**
```
{frontendBaseUrl}/first-login?token={secureToken}
```

**Example:**
```
http://localhost:3000/first-login?token=a1B2c3D4e5F6g7H8i9J0k1L2m3N4o5P6q7R8s9T0u1V2
```

---

## Configuration

### Environment Variables

| Variable | Description | Default | Required |
|----------|-------------|---------|----------|
| `FRONTEND_BASE_URL` | Base URL of frontend application | `http://localhost:3000` | No |

### Setting Environment Variables

**Development (.env file):**
```properties
FRONTEND_BASE_URL=http://localhost:3000
```

**Production (Docker/Kubernetes):**
```bash
export FRONTEND_BASE_URL=https://yourapp.com
```

**Application Properties:**
```yaml
app:
  frontend:
    base-url: ${FRONTEND_BASE_URL:http://localhost:3000}
```

---

## Replacing the Stub with Real Email Service

### Option 1: Spring Mail (SMTP)

#### 1. Add Dependency
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-mail</artifactId>
</dependency>
```

#### 2. Configure SMTP
```yaml
spring:
  mail:
    host: smtp.gmail.com
    port: 587
    username: ${SMTP_USERNAME}
    password: ${SMTP_PASSWORD}
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
```

#### 3. Create SmtpEmailService
```java
@Service
@Primary  // Override LoggingEmailService
@RequiredArgsConstructor
public class SmtpEmailService implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Override
    public void sendFirstLoginEmail(User user, String firstLoginLink) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(user.getEmail());
            helper.setSubject("Welcome! Set up your password");

            String htmlBody = """
                <html>
                <body>
                    <h2>Hello %s,</h2>
                    <p>Your account has been created. Please set up your password by clicking the link below:</p>
                    <p><a href="%s">Set up your password</a></p>
                    <p>This link is for one-time use only and does not expire.</p>
                    <p>If you did not request this account, please contact your administrator.</p>
                    <br/>
                    <p>Best regards,<br/>The Team</p>
                </body>
                </html>
                """.formatted(user.getUsername(), firstLoginLink);

            helper.setText(htmlBody, true);
            mailSender.send(message);

            log.info("First login email sent to {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send first login email to {}", user.getEmail(), e);
            throw new RuntimeException("Failed to send email", e);
        }
    }

    // ... other methods ...
}
```

---

### Option 2: SendGrid

#### 1. Add Dependency
```xml
<dependency>
    <groupId>com.sendgrid</groupId>
    <artifactId>sendgrid-java</artifactId>
    <version>4.9.3</version>
</dependency>
```

#### 2. Configure API Key
```yaml
sendgrid:
  api-key: ${SENDGRID_API_KEY}
  from-email: noreply@yourapp.com
```

#### 3. Create SendGridEmailService
```java
@Service
@Primary
public class SendGridEmailService implements EmailService {

    @Value("${sendgrid.api-key}")
    private String apiKey;

    @Value("${sendgrid.from-email}")
    private String fromEmail;

    @Override
    public void sendFirstLoginEmail(User user, String firstLoginLink) {
        Email from = new Email(fromEmail);
        String subject = "Welcome! Set up your password";
        Email to = new Email(user.getEmail());
        Content content = new Content(
            "text/html",
            String.format("<p>Hello %s,</p><p>Click here: <a href=\"%s\">Set up password</a></p>",
                user.getUsername(), firstLoginLink)
        );

        Mail mail = new Mail(from, subject, to, content);

        SendGrid sg = new SendGrid(apiKey);
        Request request = new Request();

        try {
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            Response response = sg.api(request);
            log.info("SendGrid email sent: status={}", response.getStatusCode());
        } catch (IOException e) {
            log.error("Failed to send email via SendGrid", e);
            throw new RuntimeException("Failed to send email", e);
        }
    }
}
```

---

### Option 3: AWS SES

#### 1. Add AWS SDK
```xml
<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>ses</artifactId>
    <version>2.20.0</version>
</dependency>
```

#### 2. Configure AWS Credentials
```yaml
aws:
  region: us-east-1
  ses:
    from-email: noreply@yourapp.com
```

#### 3. Create SesEmailService
```java
@Service
@Primary
public class SesEmailService implements EmailService {

    private final SesClient sesClient;

    @Value("${aws.ses.from-email}")
    private String fromEmail;

    public SesEmailService(@Value("${aws.region}") String region) {
        this.sesClient = SesClient.builder()
            .region(Region.of(region))
            .build();
    }

    @Override
    public void sendFirstLoginEmail(User user, String firstLoginLink) {
        SendEmailRequest request = SendEmailRequest.builder()
            .destination(Destination.builder()
                .toAddresses(user.getEmail())
                .build())
            .message(Message.builder()
                .subject(Content.builder()
                    .data("Welcome! Set up your password")
                    .build())
                .body(Body.builder()
                    .html(Content.builder()
                        .data(String.format(
                            "<p>Hello %s,</p><p>Click here: <a href=\"%s\">Set up password</a></p>",
                            user.getUsername(), firstLoginLink))
                        .build())
                    .build())
                .build())
            .source(fromEmail)
            .build();

        sesClient.sendEmail(request);
        log.info("SES email sent to {}", user.getEmail());
    }
}
```

---

## Testing

### Development Testing (LoggingEmailService)

When you create a user, check the console logs:

```bash
# Start application
mvn spring-boot:run

# Create a user via API
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "newuser",
    "email": "newuser@example.com",
    "password": "TempP@ss123"
  }'

# Check logs for email output
# You'll see the complete email with the first-login link
```

### Integration Testing

```java
@SpringBootTest
class EmailIntegrationTest {

    @Autowired
    private AuthService authService;

    @MockBean
    private EmailService emailService;

    @Test
    void shouldSendFirstLoginEmailWhenUserCreated() {
        // Given
        RegisterRequest request = new RegisterRequest("testuser", "test@example.com", "TempP@ss123");

        // When
        authService.register(request);

        // Then
        verify(emailService, times(1)).sendFirstLoginEmail(
            argThat(user -> user.getUsername().equals("testuser")),
            argThat(link -> link.contains("/first-login?token="))
        );
    }
}
```

---

## Complete User Creation Flow

```
1. TechAdmin calls POST /api/auth/register
   ↓
2. AuthService validates request
   ↓
3. User created with:
   - temporaryPassword = true
   - mustChangePassword = true
   - passwordHash = encoded temp password
   ↓
4. User saved to database
   ↓
5. OneTimeTokenService.createFirstLoginToken(user)
   - Generates secure 256-bit token
   - Invalidates any previous FIRST_LOGIN tokens
   - Saves token to one_time_tokens table
   ↓
6. Compose email link:
   frontendBaseUrl + "/first-login?token=" + token
   ↓
7. EmailService.sendFirstLoginEmail(user, link)
   - LoggingEmailService: logs to console
   - SmtpEmailService: sends via SMTP
   - SendGridEmailService: sends via SendGrid API
   ↓
8. User receives email
   ↓
9. User clicks link → Frontend /first-login page
   ↓
10. Frontend validates token → Backend /api/auth/first-login/validate
   ↓
11. User sets password → Backend /api/auth/first-login/set-password
   ↓
12. Token marked as used, flags cleared
   ↓
13. User can login with new password
```

---

## Security Considerations

### 1. Token Security
- ✅ 256-bit cryptographically secure random tokens
- ✅ URL-safe Base64 encoding
- ✅ One-time use (marked as used after password set)
- ✅ Previous tokens invalidated when new one created

### 2. Email Security
- ⚠️ Emails are sent over TLS/SSL (when using real SMTP)
- ⚠️ Links should use HTTPS in production
- ⚠️ Tokens don't expire (add expiry if needed)

### 3. Production Checklist
- [ ] Set `FRONTEND_BASE_URL` to production domain (HTTPS)
- [ ] Configure real email service (SMTP/SendGrid/SES)
- [ ] Remove or disable `LoggingEmailService` in production
- [ ] Set up email delivery monitoring
- [ ] Configure email templates with branding
- [ ] Add rate limiting to prevent email spam
- [ ] Consider adding token expiry (optional)

---

## Troubleshooting

### Email Not Sent
**Problem:** No email in inbox
**Solution:** Check logs for `LoggingEmailService` output - emails are logged, not sent

### Wrong Link in Email
**Problem:** Link points to wrong domain
**Solution:** Check `FRONTEND_BASE_URL` environment variable

### Token Invalid
**Problem:** Token validation fails
**Solution:**
- Check token hasn't been used already
- Verify token matches what's in database
- Check user still has temporary password flags set

---

## Files Modified/Created

### Created:
1. `service/EmailService.java` - Interface
2. `service/impl/LoggingEmailService.java` - Stub implementation

### Modified:
1. `service/AuthService.java` - Send email on user creation
2. `config/DataSeeder.java` - Send email for initial admin
3. `resources/application.yml` - Add frontend base URL config

---

## Summary

✅ **Email service implemented** with interface for flexibility
✅ **Stub implementation** logs emails to console (no SMTP needed)
✅ **Frontend URL configurable** via environment variable
✅ **First login emails sent** when TechAdmin creates users
✅ **Complete token-based flow** with secure random tokens
✅ **Production-ready architecture** - just swap `LoggingEmailService` for real implementation

**Next Steps:**
1. Choose email provider (SMTP/SendGrid/SES)
2. Add dependency and configuration
3. Implement real `EmailService`
4. Add `@Primary` annotation to override stub
5. Test with real emails! 📧

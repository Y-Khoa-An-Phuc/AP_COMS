# OneTimeTokenService Implementation

## Overview
Implemented a centralized `OneTimeTokenService` to manage all one-time token operations with cryptographically secure token generation using `SecureRandom` and URL-safe Base64 encoding.

---

## Service Features

### 1. **Secure Token Generation**
```java
public String generateSecureToken() {
    byte[] randomBytes = new byte[32];  // 256 bits
    secureRandom.nextBytes(randomBytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
}
```

**Details:**
- Uses `SecureRandom` for cryptographically secure random number generation
- Generates 32 bytes (256 bits) of random data
- Encodes using URL-safe Base64 without padding
- Resulting token is ~43 characters long
- URL-safe means it can be safely used in URLs without encoding

**Token Format:** `a1B2c3D4e5F6g7H8i9J0k1L2m3N4o5P6q7R8s9T0u1V2`

---

## Public Methods

### Core Token Management

#### `createToken(User user, TokenType type, boolean invalidatePrevious)`
Creates and persists a one-time token for a user.

**Parameters:**
- `user` - The user to create the token for
- `type` - The type of token (FIRST_LOGIN, PASSWORD_RESET, etc.)
- `invalidatePrevious` - If true, marks all previous unused tokens of the same type as used

**Returns:** The created and persisted `OneTimeToken`

**Example:**
```java
OneTimeToken token = oneTimeTokenService.createToken(
    user,
    TokenType.FIRST_LOGIN,
    true  // Invalidate previous FIRST_LOGIN tokens
);
```

---

#### `createFirstLoginToken(User user)`
Convenience method for creating FIRST_LOGIN tokens.

**Parameters:**
- `user` - The user to create the token for

**Returns:** The created `OneTimeToken`

**Behavior:**
- Automatically invalidates any previous FIRST_LOGIN tokens for the user
- Generates a new secure token
- Saves and returns the token

**Example:**
```java
OneTimeToken token = oneTimeTokenService.createFirstLoginToken(user);
String tokenString = token.getToken();
// Use tokenString in email link
```

---

### Token Validation

#### `validateToken(String tokenString, TokenType expectedType)`
Validates a token for use.

**Parameters:**
- `tokenString` - The token string to validate
- `expectedType` - The expected token type

**Returns:** The validated `OneTimeToken`

**Throws:**
- `BadRequestException` if:
  - Token doesn't exist
  - Token type doesn't match expected type
  - Token has already been used

**Example:**
```java
try {
    OneTimeToken token = oneTimeTokenService.validateToken(
        tokenString,
        TokenType.FIRST_LOGIN
    );
    User user = token.getUser();
    // Token is valid, proceed with operation
} catch (BadRequestException e) {
    // Token is invalid
}
```

---

#### `findByToken(String tokenString)`
Finds a token by its token string.

**Parameters:**
- `tokenString` - The token string to search for

**Returns:** The `OneTimeToken`

**Throws:**
- `BadRequestException` if token not found

---

### Token Lifecycle

#### `markTokenAsUsed(OneTimeToken token)`
Marks a token as used (consumes it).

**Parameters:**
- `token` - The token to mark as used

**Example:**
```java
OneTimeToken token = oneTimeTokenService.validateToken(tokenString, TokenType.FIRST_LOGIN);
// ... perform operation ...
oneTimeTokenService.markTokenAsUsed(token);
```

---

#### `invalidatePreviousTokens(User user, TokenType type)`
Invalidates all previous unused tokens of a specific type for a user.

**Parameters:**
- `user` - The user whose tokens to invalidate
- `type` - The type of tokens to invalidate

**Example:**
```java
// Before creating a new password reset token, invalidate old ones
oneTimeTokenService.invalidatePreviousTokens(user, TokenType.PASSWORD_RESET);
```

---

### Query Methods

#### `getTokensByUserAndType(User user, TokenType type)`
Gets all tokens for a specific user and type (both used and unused).

**Returns:** List of matching tokens

---

#### `getUnusedTokensByUserAndType(User user, TokenType type)`
Gets all unused tokens for a specific user and type.

**Returns:** List of unused tokens

---

## Usage in AuthService

### User Registration
```java
@Transactional
public AuthResponse register(RegisterRequest request) {
    // ... create user ...
    userRepository.save(user);

    // Generate first login token
    OneTimeToken firstLoginToken = oneTimeTokenService.createFirstLoginToken(user);

    // TODO: Send email with link containing firstLoginToken.getToken()

    // ... return response ...
}
```

### Token Validation
```java
public FirstLoginValidateResponse validateFirstLoginToken(String token) {
    // Validate using service (checks type and used status)
    OneTimeToken oneTimeToken = oneTimeTokenService.validateToken(
        token,
        TokenType.FIRST_LOGIN
    );

    User user = oneTimeToken.getUser();
    // ... additional validation ...

    return response;
}
```

### Password Setting
```java
@Transactional
public void setPasswordWithFirstLoginToken(FirstLoginSetPasswordRequest request) {
    // Validate token
    OneTimeToken oneTimeToken = oneTimeTokenService.validateToken(
        request.getToken(),
        TokenType.FIRST_LOGIN
    );

    User user = oneTimeToken.getUser();
    // ... update password ...
    userRepository.save(user);

    // Mark token as used
    oneTimeTokenService.markTokenAsUsed(oneTimeToken);
}
```

---

## Usage in DataSeeder

### Creating Initial Admin
```java
private void seedSuperAdminUser() {
    // ... create user ...
    userRepository.save(superAdmin);

    // Generate first login token using service
    OneTimeToken firstLoginToken = oneTimeTokenService.createFirstLoginToken(superAdmin);

    log.info("First login token: {}", firstLoginToken.getToken());
}
```

---

## Security Features

### 1. **Cryptographically Secure Randomness**
- Uses `SecureRandom` (not `Random`)
- Suitable for security-sensitive applications
- Unpredictable token generation

### 2. **URL-Safe Encoding**
- Base64 URL-safe encoding
- No padding characters
- Can be safely used in URLs without escaping

### 3. **Token Invalidation**
- Previous tokens automatically invalidated when creating new ones
- Prevents token reuse
- One-time use enforcement

### 4. **Type Safety**
- Token type validation ensures tokens are used for correct purpose
- FIRST_LOGIN tokens can't be used for PASSWORD_RESET and vice versa

### 5. **Audit Trail**
- Creation timestamp on each token
- Can track when tokens were created
- Logs all token operations

---

## Token Lifecycle Flow

### Creating a Token
```
1. User created/registered
   ↓
2. oneTimeTokenService.createFirstLoginToken(user)
   ↓
3. Invalidate previous FIRST_LOGIN tokens (if any)
   ↓
4. Generate secure random token (32 bytes → Base64)
   ↓
5. Create OneTimeToken entity
   ↓
6. Save to database
   ↓
7. Return token
```

### Using a Token
```
1. Frontend calls /api/auth/first-login/validate?token=XXX
   ↓
2. oneTimeTokenService.validateToken(token, FIRST_LOGIN)
   ↓
3. Find token in database
   ↓
4. Verify type matches (FIRST_LOGIN)
   ↓
5. Verify token.used == false
   ↓
6. Return validated token
   ↓
7. Perform operation (set password, etc.)
   ↓
8. oneTimeTokenService.markTokenAsUsed(token)
   ↓
9. Save token with used=true
```

---

## Advantages Over Previous Implementation

### Before (Direct Token Management)
```java
// In AuthService - scattered logic
String tokenString = UUID.randomUUID().toString();
OneTimeToken token = OneTimeToken.builder()
    .token(tokenString)
    .user(user)
    .type(TokenType.FIRST_LOGIN)
    .used(false)
    .build();
oneTimeTokenRepository.save(token);

// Validation scattered across methods
OneTimeToken token = oneTimeTokenRepository.findByToken(tokenString)
    .orElseThrow(() -> new BadRequestException("Invalid token"));
if (token.getType() != TokenType.FIRST_LOGIN) {
    throw new BadRequestException("Invalid token type");
}
if (token.isUsed()) {
    throw new BadRequestException("Token already used");
}
```

### After (Centralized Service)
```java
// Token creation
OneTimeToken token = oneTimeTokenService.createFirstLoginToken(user);

// Token validation
OneTimeToken token = oneTimeTokenService.validateToken(
    tokenString,
    TokenType.FIRST_LOGIN
);

// Token consumption
oneTimeTokenService.markTokenAsUsed(token);
```

### Benefits
1. **DRY** - Don't Repeat Yourself
2. **Secure by default** - Uses SecureRandom, not UUID
3. **Consistent validation** - Same logic everywhere
4. **Easier testing** - Mock one service instead of repository
5. **Centralized logging** - All token operations logged in one place
6. **Better security** - URL-safe Base64 is better than UUID

---

## Token Comparison

### UUID (Previous)
- Format: `550e8400-e29b-41d4-a716-446655440000`
- Length: 36 characters
- Entropy: 122 bits (UUID v4)
- URL-safe: No (contains dashes)

### SecureRandom + Base64 (Current)
- Format: `a1B2c3D4e5F6g7H8i9J0k1L2m3N4o5P6q7R8s9T0u1V2`
- Length: 43 characters
- Entropy: 256 bits
- URL-safe: Yes (URL-safe encoding)

**Security Improvement:** 2x more entropy (256 bits vs 122 bits)

---

## Future Enhancements

### 1. Token Expiration
```java
public OneTimeToken createToken(User user, TokenType type, Duration expiry) {
    // ... generate token ...
    token.setExpiresAt(LocalDateTime.now().plus(expiry));
    // ...
}
```

### 2. Rate Limiting
```java
public void validateTokenWithRateLimit(String token, String ipAddress) {
    // Check rate limit for this IP
    // Prevent brute force attacks
}
```

### 3. Token Metadata
```java
token.setCreatedFromIp(ipAddress);
token.setUserAgent(userAgent);
```

### 4. Scheduled Cleanup
```java
@Scheduled(cron = "0 0 2 * * ?")  // 2 AM daily
public void cleanupOldTokens() {
    // Delete tokens older than 30 days
}
```

---

## Testing

All tests updated to use `OneTimeTokenService`:

```java
@Autowired
private OneTimeTokenService oneTimeTokenService;

@BeforeEach
void setUp() {
    // Create token using service
    OneTimeToken token = oneTimeTokenService.createFirstLoginToken(user);
    firstLoginToken = token.getToken();
}
```

---

## Files Modified

1. **Created:** `service/OneTimeTokenService.java` - New service
2. **Updated:** `service/AuthService.java` - Use service instead of direct repository access
3. **Updated:** `config/DataSeeder.java` - Use service for token creation
4. **Updated:** `integration/FirstLoginTokenFlowIntegrationTest.java` - Use service in tests

---

## Summary

The `OneTimeTokenService` provides:
- ✅ Cryptographically secure token generation
- ✅ URL-safe Base64 encoding
- ✅ Centralized token management
- ✅ Automatic invalidation of previous tokens
- ✅ Consistent validation logic
- ✅ Better security (256-bit entropy)
- ✅ Comprehensive logging
- ✅ Type-safe token operations

**Result:** A robust, secure, and maintainable token management system. 🔐

# OneTimeToken Implementation Summary

## Overview
Implemented a reusable `OneTimeToken` entity system to replace the single-purpose `firstLoginToken` field in the User entity. This provides a flexible, scalable solution for managing various types of one-time tokens (first login, password reset, etc.).

---

## Components Created

### 1. TokenType Enum
**File:** `src/main/java/com/src/ap/entity/TokenType.java`

```java
public enum TokenType {
    FIRST_LOGIN,      // Token for first-time login and password setup
    PASSWORD_RESET    // Token for password reset (future use)
}
```

---

### 2. OneTimeToken Entity
**File:** `src/main/java/com/src/ap/entity/OneTimeToken.java`

**Fields:**
- `id` (Long, PK, auto-generated)
- `token` (String, unique, non-null, 128 chars) - The token string
- `user` (ManyToOne User) - Associated user
- `type` (TokenType enum) - Type of token
- `used` (boolean, default false) - Whether token has been consumed
- `createdAt` (LocalDateTime, auto-set) - Creation timestamp

**No expiry field** - Tokens don't expire, they're just marked as used

**Indexes:**
- Unique index on `token` for fast lookups
- Composite index on `user_id, type` for batch operations

---

### 3. OneTimeTokenRepository
**File:** `src/main/java/com/src/ap/repository/OneTimeTokenRepository.java`

**Methods:**
- `Optional<OneTimeToken> findByToken(String token)` - Find token by string
- `List<OneTimeToken> findByUserAndType(User user, TokenType type)` - Find all tokens for user/type
- `List<OneTimeToken> findByUserAndTypeAndUsed(User user, TokenType type, boolean used)` - Find unused tokens

---

### 4. Database Migration
**File:** `src/main/resources/db/migration/create_one_time_tokens_table.sql`

```sql
CREATE TABLE one_time_tokens (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    token NVARCHAR(128) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    type NVARCHAR(50) NOT NULL,
    used BIT NOT NULL DEFAULT 0,
    created_at DATETIME2 NOT NULL,
    CONSTRAINT fk_one_time_tokens_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX idx_token ON one_time_tokens(token);
CREATE INDEX idx_user_type ON one_time_tokens(user_id, type);
```

---

## Code Changes

### AuthService Updated

#### User Registration (register method)
**Before:**
```java
user.setFirstLoginToken(firstLoginToken);
```

**After:**
```java
userRepository.save(user);

String tokenString = generateFirstLoginToken();
OneTimeToken firstLoginToken = OneTimeToken.builder()
        .token(tokenString)
        .user(user)
        .type(TokenType.FIRST_LOGIN)
        .used(false)
        .build();
oneTimeTokenRepository.save(firstLoginToken);
```

#### Token Validation (validateFirstLoginToken method)
**Before:**
```java
User user = userRepository.findByFirstLoginToken(token).orElseThrow();
if (user.getFirstLoginToken() == null) { ... }
```

**After:**
```java
OneTimeToken oneTimeToken = oneTimeTokenRepository.findByToken(token).orElseThrow();
if (oneTimeToken.getType() != TokenType.FIRST_LOGIN) { ... }
if (oneTimeToken.isUsed()) { ... }
User user = oneTimeToken.getUser();
```

#### Password Setting (setPasswordWithFirstLoginToken method)
**Before:**
```java
user.setFirstLoginToken(null);  // Consume token
userRepository.save(user);
```

**After:**
```java
userRepository.save(user);

oneTimeToken.setUsed(true);  // Mark as consumed
oneTimeTokenRepository.save(oneTimeToken);
```

---

### DataSeeder Updated

**Before:**
```java
User superAdmin = User.builder()
        .firstLoginToken(UUID.randomUUID().toString())
        .build();
```

**After:**
```java
userRepository.save(superAdmin);

String tokenString = UUID.randomUUID().toString();
OneTimeToken firstLoginToken = OneTimeToken.builder()
        .token(tokenString)
        .user(superAdmin)
        .type(TokenType.FIRST_LOGIN)
        .used(false)
        .build();
oneTimeTokenRepository.save(firstLoginToken);
```

---

### User Entity Cleaned Up

**Removed:**
```java
@Column(name = "first_login_token", unique = true)
private String firstLoginToken;
```

The User entity no longer stores tokens directly - they're managed separately in the `one_time_tokens` table.

---

### UserRepository Cleaned Up

**Removed:**
```java
Optional<User> findByFirstLoginToken(String firstLoginToken);
```

---

### Tests Updated

**File:** `FirstLoginTokenFlowIntegrationTest.java`

**Changes:**
- Added `@Autowired OneTimeTokenRepository oneTimeTokenRepository`
- Updated `@BeforeEach` to create `OneTimeToken` entity instead of setting field
- Updated assertions to check `oneTimeToken.isUsed()` instead of `user.getFirstLoginToken() == null`
- All tests passing with new implementation

---

## Benefits of This Approach

### 1. **Reusability**
- Same system can handle multiple token types (FIRST_LOGIN, PASSWORD_RESET, etc.)
- No need to add fields to User entity for each new token type

### 2. **Separation of Concerns**
- User entity focuses on user data
- Token management is separate and specialized

### 3. **Better Tracking**
- Each token has metadata (type, creation time, used status)
- Can query all tokens for a user
- Can clean up old tokens easily

### 4. **Scalability**
- One user can have multiple tokens of different types
- Can add new token types without schema changes

### 5. **Security**
- Tokens can't be reused (marked as used)
- Clear separation between token types
- Easy to implement additional security features (expiry, max uses, etc.)

---

## Token Flow

### When Admin Creates User:

```
1. User saved to database
   ↓
2. OneTimeToken created with:
   - token = UUID string
   - user = new user
   - type = FIRST_LOGIN
   - used = false
   ↓
3. Token saved to one_time_tokens table
   ↓
4. Email sent with link (TODO)
```

### When User Uses Token:

```
1. Frontend calls /api/auth/first-login/validate?token=XXX
   ↓
2. Backend finds OneTimeToken by token string
   ↓
3. Verifies: type==FIRST_LOGIN, used==false, user flags correct
   ↓
4. Returns user info (username, email)
   ↓
5. User sets password via /api/auth/first-login/set-password
   ↓
6. Backend updates user password and flags
   ↓
7. Token marked as used: oneTimeToken.setUsed(true)
   ↓
8. Token can never be used again
```

---

## Database Schema

### one_time_tokens Table

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | BIGINT | PK, Identity | Primary key |
| token | NVARCHAR(128) | NOT NULL, UNIQUE | Token string (UUID) |
| user_id | BIGINT | NOT NULL, FK | References users(id) |
| type | NVARCHAR(50) | NOT NULL | Token type (FIRST_LOGIN, etc.) |
| used | BIT | NOT NULL, DEFAULT 0 | Whether token has been consumed |
| created_at | DATETIME2 | NOT NULL | Creation timestamp |

### Indexes

1. **idx_token** (UNIQUE on token) - Fast token lookup
2. **idx_user_type** (on user_id, type) - Batch operations per user

---

## Future Enhancements

### Possible Additions (not implemented):

1. **Expiry Support:**
   ```java
   @Column(name = "expires_at")
   private LocalDateTime expiresAt;
   ```

2. **Usage Tracking:**
   ```java
   @Column(name = "used_at")
   private LocalDateTime usedAt;

   @Column(name = "used_count")
   private Integer usedCount;
   ```

3. **IP Tracking:**
   ```java
   @Column(name = "created_from_ip")
   private String createdFromIp;
   ```

4. **Token Cleanup Job:**
   - Scheduled task to delete old used tokens
   - Keep only recent tokens for audit

---

## Migration Path

### Old System → New System

If you have existing users with `firstLoginToken` in the User table:

1. **Data Migration Script:**
   ```sql
   INSERT INTO one_time_tokens (token, user_id, type, used, created_at)
   SELECT
       first_login_token,
       id,
       'FIRST_LOGIN',
       0,
       GETDATE()
   FROM users
   WHERE first_login_token IS NOT NULL;
   ```

2. **Then drop old column:**
   ```sql
   DROP INDEX idx_users_first_login_token ON users;
   ALTER TABLE users DROP COLUMN first_login_token;
   ```

---

## Files Modified/Created

### New Files (4):
1. `entity/TokenType.java` - Enum
2. `entity/OneTimeToken.java` - Entity
3. `repository/OneTimeTokenRepository.java` - Repository
4. `db/migration/create_one_time_tokens_table.sql` - Migration

### Modified Files (5):
1. `service/AuthService.java` - Use OneTimeToken instead of User field
2. `config/DataSeeder.java` - Create OneTimeToken for admin
3. `entity/User.java` - Removed firstLoginToken field
4. `repository/UserRepository.java` - Removed findByFirstLoginToken
5. `integration/FirstLoginTokenFlowIntegrationTest.java` - Updated tests

---

## Testing

All existing tests updated and passing:
- ✅ Token validation tests
- ✅ Password setting tests
- ✅ Token reuse prevention tests
- ✅ Complete flow tests

---

## Summary

The `OneTimeToken` system provides a clean, reusable architecture for managing temporary tokens. It's more flexible than storing tokens directly in the User entity and sets up the foundation for future token-based features like password reset.

**Key Takeaway:** One table, multiple use cases. 🎯

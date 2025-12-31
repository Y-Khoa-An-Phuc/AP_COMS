package com.src.ap.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.src.ap.dto.auth.AuthResponse;
import com.src.ap.dto.auth.ChangeTemporaryPasswordRequest;
import com.src.ap.dto.auth.LoginRequest;
import com.src.ap.dto.auth.PasswordChangeRequiredResponse;
import com.src.ap.dto.common.ApiResponse;
import com.src.ap.entity.Role;
import com.src.ap.entity.RoleName;
import com.src.ap.entity.User;
import com.src.ap.repository.RoleRepository;
import com.src.ap.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Temporary Password Flow Integration Tests")
class TemporaryPasswordFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final String TEMP_USERNAME = "tempuser";
    private static final String TEMP_PASSWORD = "TempP@ss123";
    private static final String NEW_PASSWORD = "NewSecure@Pass456";
    private static final String TEMP_EMAIL = "tempuser@example.com";

    private User tempPasswordUser;
    private User normalUser;
    private Role userRole;

    @BeforeEach
    void setUp() {
        // Clean up any existing test data
        userRepository.deleteAll();

        // Ensure USER role exists
        userRole = roleRepository.findByName(RoleName.USER)
                .orElseGet(() -> roleRepository.save(Role.builder()
                        .name(RoleName.USER)
                        .build()));

        // Create user with temporary password
        tempPasswordUser = User.builder()
                .username(TEMP_USERNAME)
                .passwordHash(passwordEncoder.encode(TEMP_PASSWORD))
                .email(TEMP_EMAIL)
                .roles(Set.of(userRole))
                .enabled(true)
                .locked(false)
                .mustChangePassword(true)
                .temporaryPassword(true)
                .tokenVersion(0)
                .failedLoginAttempts(0)
                .build();

        userRepository.save(tempPasswordUser);

        // Create normal user for comparison tests
        normalUser = User.builder()
                .username("normaluser")
                .passwordHash(passwordEncoder.encode("NormalP@ss123"))
                .email("normaluser@example.com")
                .roles(Set.of(userRole))
                .enabled(true)
                .locked(false)
                .mustChangePassword(false)
                .temporaryPassword(false)
                .tokenVersion(0)
                .failedLoginAttempts(0)
                .build();

        userRepository.save(normalUser);
    }

    // ========== LOGIN FLOW TESTS ==========

    @Test
    @DisplayName("Given user with mustChangePassword=true and correct password, should now return 200 with JWT (old behavior removed)")
    void givenUserWithMustChangePasswordTrueAndCorrectPassword_shouldNowReturn200WithJwt() throws Exception {
        // NOTE: This test has been updated for the new token-based first-login flow.
        // The old behavior (returning 403 for mustChangePassword=true) has been removed.
        // Users should now use the first-login token flow instead.

        // Given: User with mustChangePassword=true (temporary password user)
        LoginRequest loginRequest = new LoginRequest(TEMP_USERNAME, TEMP_PASSWORD);

        // When: POST /api/auth/login with correct credentials
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                // Then: Should now return 200 OK with JWT (behavior changed)
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Login successful"))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andReturn();

        // Verify: Response contains JWT token
        String responseJson = result.getResponse().getContentAsString();
        ApiResponse<AuthResponse> apiResponse = objectMapper.readValue(
                responseJson,
                objectMapper.getTypeFactory().constructParametricType(
                        ApiResponse.class,
                        AuthResponse.class
                )
        );

        // Assert: JWT is present even though user has mustChangePassword=true
        assertThat(apiResponse.isSuccess()).isTrue();
        assertThat(apiResponse.getData()).isNotNull();
        assertThat(apiResponse.getData().getToken()).isNotNull().isNotEmpty();
    }

    @Test
    @DisplayName("Given user with mustChangePassword=false and correct password, should return 200 with JWT")
    void givenUserWithMustChangePasswordFalseAndCorrectPassword_shouldReturn200WithJwt() throws Exception {
        // Given: User with mustChangePassword=false (normal user)
        LoginRequest loginRequest = new LoginRequest("normaluser", "NormalP@ss123");

        // When: POST /api/auth/login with correct credentials
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                // Then: Should return 200 OK
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Login successful"))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.type").value("Bearer"))
                .andExpect(jsonPath("$.data.username").value("normaluser"))
                .andExpect(jsonPath("$.data.email").value("normaluser@example.com"))
                .andReturn();

        // Verify: Response contains JWT token
        String responseJson = result.getResponse().getContentAsString();
        ApiResponse<AuthResponse> apiResponse = objectMapper.readValue(
                responseJson,
                objectMapper.getTypeFactory().constructParametricType(
                        ApiResponse.class,
                        AuthResponse.class
                )
        );

        // Assert: JWT token is present and valid
        assertThat(apiResponse.isSuccess()).isTrue();
        assertThat(apiResponse.getData()).isNotNull();
        assertThat(apiResponse.getData().getToken()).isNotNull().isNotEmpty();
        assertThat(apiResponse.getData().getType()).isEqualTo("Bearer");
        assertThat(apiResponse.getData().getUsername()).isEqualTo("normaluser");

        // Verify: Response does NOT contain mustChangePassword field
        assertThat(responseJson).doesNotContain("mustChangePassword");
    }

    // ========== TEMPORARY PASSWORD LOGIN TESTS ==========

    @Test
    @DisplayName("Should return 200 OK when login with temporary password (behavior changed)")
    void shouldReturn200OkWhenLoginWithTemporaryPassword() throws Exception {
        // NOTE: This test has been updated for the new token-based first-login flow.
        // The old behavior (returning 403) has been removed.

        // Given: User with temporary password
        LoginRequest loginRequest = new LoginRequest(TEMP_USERNAME, TEMP_PASSWORD);

        // When: POST /api/auth/login
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                // Then: Should now return 200 OK with JWT
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Login successful"))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andReturn();

        // Verify response structure
        String responseJson = result.getResponse().getContentAsString();
        ApiResponse<AuthResponse> apiResponse = objectMapper.readValue(
                responseJson,
                objectMapper.getTypeFactory().constructParametricType(
                        ApiResponse.class,
                        AuthResponse.class
                )
        );

        assertThat(apiResponse.isSuccess()).isTrue();
        assertThat(apiResponse.getData().getToken()).isNotNull().isNotEmpty();
    }

    @Test
    @DisplayName("Should allow normal user to login and receive JWT token")
    void shouldAllowNormalUserToLoginAndReceiveJwtToken() throws Exception {
        // Given: Normal user credentials
        LoginRequest loginRequest = new LoginRequest("normaluser", "NormalP@ss123");

        // When: POST /api/auth/login
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                // Then: Should return 200 OK with JWT
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.type").value("Bearer"));
    }

    @Test
    @DisplayName("Should return 401 when login with temporary password but wrong credentials")
    void shouldReturn401WhenLoginWithTemporaryPasswordButWrongCredentials() throws Exception {
        // Given: Invalid credentials for temporary password user
        LoginRequest loginRequest = new LoginRequest(TEMP_USERNAME, "WrongPassword");

        // When: POST /api/auth/login
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                // Then: Should return 401 Unauthorized (not 403)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Tên đăng nhập hoặc mật khẩu không đúng."));
    }

    // ========== FIRST-LOGIN CHANGE PASSWORD TESTS ==========

    @Test
    @DisplayName("Given valid temporary password and strong new password, should return 200 success and flags changed to false")
    void givenValidTemporaryPasswordAndStrongNewPassword_shouldReturn200SuccessAndFlagsChangedToFalse() throws Exception {
        // Given: User with temporary password and valid change request
        ChangeTemporaryPasswordRequest request = new ChangeTemporaryPasswordRequest(
                TEMP_USERNAME,
                TEMP_PASSWORD,
                NEW_PASSWORD,
                NEW_PASSWORD
        );

        // When: POST /api/auth/change-temporary-password
        mockMvc.perform(post("/api/auth/change-temporary-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                // Then: Should return 200 OK
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Password changed successfully. Please login again with your new password."))
                .andExpect(jsonPath("$.data").doesNotExist());

        // Verify: Both flags are changed to false in database
        User updatedUser = userRepository.findByUsername(TEMP_USERNAME).orElseThrow();
        assertThat(updatedUser.isMustChangePassword()).isFalse();
        assertThat(updatedUser.isTemporaryPassword()).isFalse();

        // Verify: User can now login with new password and receive JWT
        LoginRequest loginRequest = new LoginRequest(TEMP_USERNAME, NEW_PASSWORD);
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").isNotEmpty());
    }

    @Test
    @DisplayName("Given invalid current password, should return 401 Unauthorized")
    void givenInvalidCurrentPassword_shouldReturn401Unauthorized() throws Exception {
        // Given: Invalid current password
        ChangeTemporaryPasswordRequest request = new ChangeTemporaryPasswordRequest(
                TEMP_USERNAME,
                "WrongPassword123",
                NEW_PASSWORD,
                NEW_PASSWORD
        );

        // When: POST /api/auth/change-temporary-password
        mockMvc.perform(post("/api/auth/change-temporary-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                // Then: Should return 401 Unauthorized
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Tên đăng nhập hoặc mật khẩu không đúng."));

        // Verify: Flags remain unchanged in database
        User unchangedUser = userRepository.findByUsername(TEMP_USERNAME).orElseThrow();
        assertThat(unchangedUser.isMustChangePassword()).isTrue();
        assertThat(unchangedUser.isTemporaryPassword()).isTrue();
    }

    @Test
    @DisplayName("Given weak new password (too short), should return 400 with validation details")
    void givenWeakNewPasswordTooShort_shouldReturn400WithValidationDetails() throws Exception {
        // Given: New password less than 8 characters
        ChangeTemporaryPasswordRequest request = new ChangeTemporaryPasswordRequest(
                TEMP_USERNAME,
                TEMP_PASSWORD,
                "Short1",
                "Short1"
        );

        // When: POST /api/auth/change-temporary-password
        mockMvc.perform(post("/api/auth/change-temporary-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                // Then: Should return 400 Bad Request with validation details
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.data.newPassword").value("Password must be at least 8 characters long"));

        // Verify: Flags remain unchanged in database
        User unchangedUser = userRepository.findByUsername(TEMP_USERNAME).orElseThrow();
        assertThat(unchangedUser.isMustChangePassword()).isTrue();
        assertThat(unchangedUser.isTemporaryPassword()).isTrue();
    }

    @Test
    @DisplayName("Given user not in mustChangePassword state, should return 400 Bad Request")
    void givenUserNotInMustChangePasswordState_shouldReturn400BadRequest() throws Exception {
        // Given: Normal user (not in temporary password state)
        ChangeTemporaryPasswordRequest request = new ChangeTemporaryPasswordRequest(
                "normaluser",
                "NormalP@ss123",
                NEW_PASSWORD,
                NEW_PASSWORD
        );

        // When: POST /api/auth/change-temporary-password
        mockMvc.perform(post("/api/auth/change-temporary-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                // Then: Should return 400 Bad Request
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Password has already been changed"));

        // Verify: Normal user's flags remain false
        User normalUser = userRepository.findByUsername("normaluser").orElseThrow();
        assertThat(normalUser.isMustChangePassword()).isFalse();
        assertThat(normalUser.isTemporaryPassword()).isFalse();
    }

    // ========== ADDITIONAL CHANGE PASSWORD VALIDATION TESTS ==========

    @Test
    @DisplayName("Should successfully change temporary password and return 200 OK without JWT")
    void shouldSuccessfullyChangeTemporaryPasswordAndReturn200OkWithoutJwt() throws Exception {
        // Given: Valid temporary password change request
        ChangeTemporaryPasswordRequest request = new ChangeTemporaryPasswordRequest(
                TEMP_USERNAME,
                TEMP_PASSWORD,
                NEW_PASSWORD,
                NEW_PASSWORD
        );

        // When: POST /api/auth/change-temporary-password
        mockMvc.perform(post("/api/auth/change-temporary-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                // Then: Should return 200 OK without JWT token
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Password changed successfully. Please login again with your new password."))
                .andExpect(jsonPath("$.data").doesNotExist());

        // Verify: Flags are cleared in database
        User updatedUser = userRepository.findByUsername(TEMP_USERNAME).orElseThrow();
        assertThat(updatedUser.isMustChangePassword()).isFalse();
        assertThat(updatedUser.isTemporaryPassword()).isFalse();
    }

    @Test
    @DisplayName("Should allow login with new password after changing temporary password")
    void shouldAllowLoginWithNewPasswordAfterChangingTemporaryPassword() throws Exception {
        // Given: User changed temporary password
        ChangeTemporaryPasswordRequest changeRequest = new ChangeTemporaryPasswordRequest(
                TEMP_USERNAME,
                TEMP_PASSWORD,
                NEW_PASSWORD,
                NEW_PASSWORD
        );

        mockMvc.perform(post("/api/auth/change-temporary-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changeRequest)))
                .andExpect(status().isOk());

        // When: Login with new password
        LoginRequest loginRequest = new LoginRequest(TEMP_USERNAME, NEW_PASSWORD);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                // Then: Should return 200 OK with JWT token
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.type").value("Bearer"));
    }

    @Test
    @DisplayName("Should return 401 when trying to change temporary password with wrong current password")
    void shouldReturn401WhenTryingToChangeTemporaryPasswordWithWrongCurrentPassword() throws Exception {
        // Given: Invalid current password
        ChangeTemporaryPasswordRequest request = new ChangeTemporaryPasswordRequest(
                TEMP_USERNAME,
                "WrongPassword",
                NEW_PASSWORD,
                NEW_PASSWORD
        );

        // When: POST /api/auth/change-temporary-password
        mockMvc.perform(post("/api/auth/change-temporary-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                // Then: Should return 401 Unauthorized
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Tên đăng nhập hoặc mật khẩu không đúng."));
    }

    @Test
    @DisplayName("Should return 400 when new password and confirmation do not match")
    void shouldReturn400WhenNewPasswordAndConfirmationDoNotMatch() throws Exception {
        // Given: Mismatched passwords
        ChangeTemporaryPasswordRequest request = new ChangeTemporaryPasswordRequest(
                TEMP_USERNAME,
                TEMP_PASSWORD,
                NEW_PASSWORD,
                "DifferentPassword123"
        );

        // When: POST /api/auth/change-temporary-password
        mockMvc.perform(post("/api/auth/change-temporary-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                // Then: Should return 400 Bad Request
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("New password and confirmation do not match"));
    }

    @Test
    @DisplayName("Should return 400 when new password is same as temporary password")
    void shouldReturn400WhenNewPasswordIsSameAsTemporaryPassword() throws Exception {
        // Given: New password same as current
        ChangeTemporaryPasswordRequest request = new ChangeTemporaryPasswordRequest(
                TEMP_USERNAME,
                TEMP_PASSWORD,
                TEMP_PASSWORD,
                TEMP_PASSWORD
        );

        // When: POST /api/auth/change-temporary-password
        mockMvc.perform(post("/api/auth/change-temporary-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                // Then: Should return 400 Bad Request
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("New password must be different from temporary password"));
    }

    @Test
    @DisplayName("Should return 400 when trying to change password for user who already changed it")
    void shouldReturn400WhenTryingToChangePasswordForUserWhoAlreadyChangedIt() throws Exception {
        // Given: Normal user (flags are false)
        ChangeTemporaryPasswordRequest request = new ChangeTemporaryPasswordRequest(
                "normaluser",
                "NormalP@ss123",
                NEW_PASSWORD,
                NEW_PASSWORD
        );

        // When: POST /api/auth/change-temporary-password
        mockMvc.perform(post("/api/auth/change-temporary-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                // Then: Should return 400 Bad Request
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Password has already been changed"));
    }

    @Test
    @DisplayName("Should return 400 when new password is too short")
    void shouldReturn400WhenNewPasswordIsTooShort() throws Exception {
        // Given: Password less than 8 characters
        ChangeTemporaryPasswordRequest request = new ChangeTemporaryPasswordRequest(
                TEMP_USERNAME,
                TEMP_PASSWORD,
                "Short1",
                "Short1"
        );

        // When: POST /api/auth/change-temporary-password
        mockMvc.perform(post("/api/auth/change-temporary-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                // Then: Should return 400 Bad Request with validation error
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.data.newPassword").value("Password must be at least 8 characters long"));
    }

    // ========== SECURITY TESTS: ACCESS CONTROL ==========

    @Test
    @DisplayName("User without JWT cannot access protected endpoint: POST /api/auth/logout")
    void userWithoutJwtCannotAccessProtectedEndpointLogout() throws Exception {
        // Given: No JWT token (no Authorization header)

        // When: POST /api/auth/logout without JWT
        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON))
                // Then: Should return 401 Unauthorized
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("User without JWT cannot access protected endpoint: POST /api/auth/change-password")
    void userWithoutJwtCannotAccessProtectedEndpointChangePassword() throws Exception {
        // Given: No JWT token (no Authorization header)

        // When: POST /api/auth/change-password without JWT
        mockMvc.perform(post("/api/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"test\",\"newPassword\":\"NewP@ss123\",\"confirmPassword\":\"NewP@ss123\"}"))
                // Then: Should return 401 Unauthorized
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("User with invalid JWT cannot access protected endpoint")
    void userWithInvalidJwtCannotAccessProtectedEndpoint() throws Exception {
        // Given: Invalid JWT token
        String invalidToken = "invalid.jwt.token.here";

        // When: POST /api/auth/logout with invalid JWT
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + invalidToken)
                        .contentType(MediaType.APPLICATION_JSON))
                // Then: Should return 401 Unauthorized
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("User with malformed Authorization header cannot access protected endpoint")
    void userWithMalformedAuthorizationHeaderCannotAccessProtectedEndpoint() throws Exception {
        // Given: Malformed Authorization header (missing "Bearer " prefix)
        LoginRequest loginRequest = new LoginRequest("normaluser", "NormalP@ss123");

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String responseJson = loginResult.getResponse().getContentAsString();
        ApiResponse<AuthResponse> apiResponse = objectMapper.readValue(
                responseJson,
                objectMapper.getTypeFactory().constructParametricType(
                        ApiResponse.class,
                        AuthResponse.class
                )
        );
        String jwtToken = apiResponse.getData().getToken();

        // When: POST /api/auth/logout without "Bearer " prefix
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", jwtToken)  // Missing "Bearer " prefix
                        .contentType(MediaType.APPLICATION_JSON))
                // Then: Should return 401 Unauthorized
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Public endpoint /api/auth/login is accessible without JWT")
    void publicEndpointLoginIsAccessibleWithoutJwt() throws Exception {
        // Given: No JWT token (no Authorization header)
        LoginRequest loginRequest = new LoginRequest("normaluser", "NormalP@ss123");

        // When: POST /api/auth/login without JWT (public endpoint)
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                // Then: Should return 200 OK (accessible without JWT)
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").isNotEmpty());
    }

    @Test
    @DisplayName("Public endpoint /api/auth/change-temporary-password is accessible without JWT")
    void publicEndpointChangeTemporaryPasswordIsAccessibleWithoutJwt() throws Exception {
        // Given: No JWT token (no Authorization header)
        ChangeTemporaryPasswordRequest request = new ChangeTemporaryPasswordRequest(
                TEMP_USERNAME,
                TEMP_PASSWORD,
                NEW_PASSWORD,
                NEW_PASSWORD
        );

        // When: POST /api/auth/change-temporary-password without JWT (public endpoint)
        mockMvc.perform(post("/api/auth/change-temporary-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                // Then: Should return 200 OK (accessible without JWT)
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("Verify temporary password user cannot access protected endpoints without JWT")
    void verifyTemporaryPasswordUserCannotAccessProtectedEndpointsWithoutJwt() throws Exception {
        // Given: Temporary password user exists (but has no JWT)
        // This simulates the state after login returns 403 - user has credentials but no JWT

        // When: Try to access various protected endpoints

        // 1. Try POST /api/auth/logout
        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());

        // 2. Try POST /api/auth/change-password
        mockMvc.perform(post("/api/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"test\",\"newPassword\":\"NewP@ss123\",\"confirmPassword\":\"NewP@ss123\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Valid JWT allows access to protected endpoint")
    void validJwtAllowsAccessToProtectedEndpoint() throws Exception {
        // Given: User logs in and receives valid JWT
        LoginRequest loginRequest = new LoginRequest("normaluser", "NormalP@ss123");

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String responseJson = loginResult.getResponse().getContentAsString();
        ApiResponse<AuthResponse> apiResponse = objectMapper.readValue(
                responseJson,
                objectMapper.getTypeFactory().constructParametricType(
                        ApiResponse.class,
                        AuthResponse.class
                )
        );
        String jwtToken = apiResponse.getData().getToken();

        // When: POST /api/auth/logout with valid JWT
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON))
                // Then: Should return 200 OK (access granted)
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("Should deny access to protected endpoint for temporary password user")
    void shouldDenyAccessToProtectedEndpointForTemporaryPasswordUser() throws Exception {
        // When: Try to access protected endpoint without JWT (temp password users have no JWT)
        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON))
                // Then: Should return 401 Unauthorized (no JWT)
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should verify temp password user cannot call change-password endpoint")
    void shouldVerifyTempPasswordUserCannotCallChangePasswordEndpoint() throws Exception {
        // When: Try to access /api/auth/change-password without JWT
        mockMvc.perform(post("/api/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"test\",\"newPassword\":\"test\",\"confirmPassword\":\"test\"}"))
                // Then: Should return 401 Unauthorized (requires authentication)
                .andExpect(status().isUnauthorized());
    }

    // ========== COMPLETE FLOW TEST ==========

    @Test
    @DisplayName("Should complete full temporary password flow using deprecated endpoint: change password -> login -> access protected")
    void shouldCompleteFullTemporaryPasswordFlow() throws Exception {
        // NOTE: This test has been updated - STEP 1 (login returning 403) has been removed
        // as that behavior no longer exists. Users should use the first-login token flow instead.

        // ===== STEP 1: Change temporary password (using deprecated endpoint) =====
        ChangeTemporaryPasswordRequest changeRequest = new ChangeTemporaryPasswordRequest(
                TEMP_USERNAME,
                TEMP_PASSWORD,
                NEW_PASSWORD,
                NEW_PASSWORD
        );

        mockMvc.perform(post("/api/auth/change-temporary-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changeRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // ===== STEP 2: Login with new password -> 200 + JWT =====
        LoginRequest newLogin = new LoginRequest(TEMP_USERNAME, NEW_PASSWORD);

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newLogin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andReturn();

        String responseJson = loginResult.getResponse().getContentAsString();
        ApiResponse<AuthResponse> apiResponse = objectMapper.readValue(
                responseJson,
                objectMapper.getTypeFactory().constructParametricType(
                        ApiResponse.class,
                        AuthResponse.class
                )
        );
        String jwtToken = apiResponse.getData().getToken();

        // ===== STEP 3: Access protected endpoint with JWT =====
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // ===== STEP 4: Verify old password no longer works =====
        LoginRequest oldPasswordLogin = new LoginRequest(TEMP_USERNAME, TEMP_PASSWORD);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(oldPasswordLogin)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Tên đăng nhập hoặc mật khẩu không đúng."));
    }
}
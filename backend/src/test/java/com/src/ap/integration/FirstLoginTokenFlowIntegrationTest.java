package com.src.ap.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.src.ap.dto.auth.FirstLoginSetPasswordRequest;
import com.src.ap.dto.auth.FirstLoginValidateResponse;
import com.src.ap.dto.auth.LoginRequest;
import com.src.ap.dto.common.ApiResponse;
import com.src.ap.entity.OneTimeToken;
import com.src.ap.entity.Role;
import com.src.ap.entity.RoleName;
import com.src.ap.entity.TokenType;
import com.src.ap.entity.User;
import com.src.ap.repository.OneTimeTokenRepository;
import com.src.ap.repository.RoleRepository;
import com.src.ap.repository.UserRepository;
import com.src.ap.service.OneTimeTokenService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("First Login Token Flow Integration Tests")
class FirstLoginTokenFlowIntegrationTest {

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
    private OneTimeTokenRepository oneTimeTokenRepository;

    @Autowired
    private OneTimeTokenService oneTimeTokenService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final String TEST_USERNAME = "newuser";
    private static final String TEMP_PASSWORD = "TempP@ss123";
    private static final String NEW_PASSWORD = "NewSecure@Pass456";
    private static final String TEST_EMAIL = "newuser@example.com";

    private User newUser;
    private String firstLoginToken;
    private Role userRole;

    @BeforeEach
    void setUp() {
        // Clean up any existing test data
        oneTimeTokenRepository.deleteAll();
        userRepository.deleteAll();

        // Ensure USER role exists
        userRole = roleRepository.findByName(RoleName.USER)
                .orElseGet(() -> roleRepository.save(Role.builder()
                        .name(RoleName.USER)
                        .build()));

        // Create user with temporary password
        newUser = User.builder()
                .username(TEST_USERNAME)
                .passwordHash(passwordEncoder.encode(TEMP_PASSWORD))
                .email(TEST_EMAIL)
                .roles(Set.of(userRole))
                .enabled(true)
                .locked(false)
                .mustChangePassword(true)
                .temporaryPassword(true)
                .tokenVersion(0)
                .failedLoginAttempts(0)
                .build();

        userRepository.save(newUser);

        // Generate and save a one-time token for first login using the service
        OneTimeToken token = oneTimeTokenService.createFirstLoginToken(newUser);
        firstLoginToken = token.getToken();
    }

    // ========== TOKEN VALIDATION TESTS ==========

    @Test
    @DisplayName("Should validate first login token successfully")
    void shouldValidateFirstLoginTokenSuccessfully() throws Exception {
        // When: GET /api/auth/first-login/validate?token=VALID_TOKEN
        MvcResult result = mockMvc.perform(get("/api/auth/first-login/validate")
                        .param("token", firstLoginToken))
                // Then: Should return 200 OK with user information
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Token is valid"))
                .andExpect(jsonPath("$.data.valid").value(true))
                .andExpect(jsonPath("$.data.username").value(TEST_USERNAME))
                .andExpect(jsonPath("$.data.email").value(TEST_EMAIL))
                .andReturn();

        // Verify response structure
        String responseJson = result.getResponse().getContentAsString();
        ApiResponse<FirstLoginValidateResponse> apiResponse = objectMapper.readValue(
                responseJson,
                objectMapper.getTypeFactory().constructParametricType(
                        ApiResponse.class,
                        FirstLoginValidateResponse.class
                )
        );

        assertThat(apiResponse.isSuccess()).isTrue();
        assertThat(apiResponse.getData().isValid()).isTrue();
        assertThat(apiResponse.getData().getUsername()).isEqualTo(TEST_USERNAME);
        assertThat(apiResponse.getData().getEmail()).isEqualTo(TEST_EMAIL);
    }

    @Test
    @DisplayName("Should return 400 when validating invalid token")
    void shouldReturn400WhenValidatingInvalidToken() throws Exception {
        // Given: Invalid token
        String invalidToken = "invalid-token-123";

        // When: GET /api/auth/first-login/validate?token=INVALID_TOKEN
        mockMvc.perform(get("/api/auth/first-login/validate")
                        .param("token", invalidToken))
                // Then: Should return 400 Bad Request
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid or expired token"));
    }

    @Test
    @DisplayName("Should return 400 when validating already used token")
    void shouldReturn400WhenValidatingAlreadyUsedToken() throws Exception {
        // Given: Token has been marked as used
        OneTimeToken token = oneTimeTokenRepository.findByToken(firstLoginToken).orElseThrow();
        token.setUsed(true);
        oneTimeTokenRepository.save(token);

        // When: GET /api/auth/first-login/validate?token=USED_TOKEN
        mockMvc.perform(get("/api/auth/first-login/validate")
                        .param("token", firstLoginToken))
                // Then: Should return 400 Bad Request
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Token has already been used or is no longer valid"));
    }

    // ========== SET PASSWORD TESTS ==========

    @Test
    @DisplayName("Should set password successfully with valid token")
    void shouldSetPasswordSuccessfullyWithValidToken() throws Exception {
        // Given: Valid token and password request
        FirstLoginSetPasswordRequest request = new FirstLoginSetPasswordRequest(
                firstLoginToken,
                NEW_PASSWORD,
                NEW_PASSWORD
        );

        // When: POST /api/auth/first-login/set-password
        mockMvc.perform(post("/api/auth/first-login/set-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                // Then: Should return 200 OK
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Password set successfully. You are now logged in."));

        // Verify: Flags are cleared and token is marked as used
        User updatedUser = userRepository.findByUsername(TEST_USERNAME).orElseThrow();
        assertThat(updatedUser.isMustChangePassword()).isFalse();
        assertThat(updatedUser.isTemporaryPassword()).isFalse();

        OneTimeToken usedToken = oneTimeTokenRepository.findByToken(firstLoginToken).orElseThrow();
        assertThat(usedToken.isUsed()).isTrue();

        // Verify: User can login with new password
        LoginRequest loginRequest = new LoginRequest(TEST_USERNAME, NEW_PASSWORD);
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").isNotEmpty());
    }

    @Test
    @DisplayName("Should return 400 when setting password with invalid token")
    void shouldReturn400WhenSettingPasswordWithInvalidToken() throws Exception {
        // Given: Invalid token
        FirstLoginSetPasswordRequest request = new FirstLoginSetPasswordRequest(
                "invalid-token",
                NEW_PASSWORD,
                NEW_PASSWORD
        );

        // When: POST /api/auth/first-login/set-password
        mockMvc.perform(post("/api/auth/first-login/set-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                // Then: Should return 400 Bad Request
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid or expired token"));

        // Verify: User flags remain unchanged and token is not used
        User unchangedUser = userRepository.findByUsername(TEST_USERNAME).orElseThrow();
        assertThat(unchangedUser.isMustChangePassword()).isTrue();
        assertThat(unchangedUser.isTemporaryPassword()).isTrue();

        OneTimeToken unchangedToken = oneTimeTokenRepository.findByToken(firstLoginToken).orElseThrow();
        assertThat(unchangedToken.isUsed()).isFalse();
    }

    @Test
    @DisplayName("Should return 400 when passwords do not match")
    void shouldReturn400WhenPasswordsDoNotMatch() throws Exception {
        // Given: Mismatched passwords
        FirstLoginSetPasswordRequest request = new FirstLoginSetPasswordRequest(
                firstLoginToken,
                NEW_PASSWORD,
                "DifferentPassword123"
        );

        // When: POST /api/auth/first-login/set-password
        mockMvc.perform(post("/api/auth/first-login/set-password")
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
        // Given: New password same as temporary password
        FirstLoginSetPasswordRequest request = new FirstLoginSetPasswordRequest(
                firstLoginToken,
                TEMP_PASSWORD,
                TEMP_PASSWORD
        );

        // When: POST /api/auth/first-login/set-password
        mockMvc.perform(post("/api/auth/first-login/set-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                // Then: Should return 400 Bad Request
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("New password must be different from temporary password"));
    }

    @Test
    @DisplayName("Should return 400 when password is too short")
    void shouldReturn400WhenPasswordIsTooShort() throws Exception {
        // Given: Password less than 8 characters
        FirstLoginSetPasswordRequest request = new FirstLoginSetPasswordRequest(
                firstLoginToken,
                "Short1",
                "Short1"
        );

        // When: POST /api/auth/first-login/set-password
        mockMvc.perform(post("/api/auth/first-login/set-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                // Then: Should return 400 Bad Request with validation error
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.data.newPassword").value("Password must be at least 8 characters long"));
    }

    @Test
    @DisplayName("Should return 400 when trying to use token twice")
    void shouldReturn400WhenTryingToUseTokenTwice() throws Exception {
        // Given: Token used once successfully
        FirstLoginSetPasswordRequest firstRequest = new FirstLoginSetPasswordRequest(
                firstLoginToken,
                NEW_PASSWORD,
                NEW_PASSWORD
        );

        mockMvc.perform(post("/api/auth/first-login/set-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(firstRequest)))
                .andExpect(status().isOk());

        // When: Try to use the same token again
        FirstLoginSetPasswordRequest secondRequest = new FirstLoginSetPasswordRequest(
                firstLoginToken,
                "AnotherP@ss789",
                "AnotherP@ss789"
        );

        mockMvc.perform(post("/api/auth/first-login/set-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(secondRequest)))
                // Then: Should return 400 Bad Request
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Token has already been used or is no longer valid"));
    }

    // ========== COMPLETE FLOW TEST ==========

    @Test
    @DisplayName("Should complete full first login flow: validate token -> set password -> login")
    void shouldCompleteFullFirstLoginFlow() throws Exception {
        // ===== STEP 1: Validate token =====
        mockMvc.perform(get("/api/auth/first-login/validate")
                        .param("token", firstLoginToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.valid").value(true))
                .andExpect(jsonPath("$.data.username").value(TEST_USERNAME));

        // ===== STEP 2: Set password with token =====
        FirstLoginSetPasswordRequest setPasswordRequest = new FirstLoginSetPasswordRequest(
                firstLoginToken,
                NEW_PASSWORD,
                NEW_PASSWORD
        );

        mockMvc.perform(post("/api/auth/first-login/set-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(setPasswordRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // ===== STEP 3: Login with new password =====
        LoginRequest loginRequest = new LoginRequest(TEST_USERNAME, NEW_PASSWORD);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.type").value("Bearer"))
                .andExpect(jsonPath("$.data.username").value(TEST_USERNAME));

        // ===== STEP 4: Verify token cannot be used again =====
        mockMvc.perform(get("/api/auth/first-login/validate")
                        .param("token", firstLoginToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Token has already been used or is no longer valid"));

        // ===== STEP 5: Verify old password no longer works =====
        LoginRequest oldPasswordLogin = new LoginRequest(TEST_USERNAME, TEMP_PASSWORD);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(oldPasswordLogin)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Tên đăng nhập hoặc mật khẩu không đúng."));
    }

    // ========== PUBLIC ENDPOINT ACCESSIBILITY TESTS ==========

    @Test
    @DisplayName("Should allow access to validate endpoint without JWT")
    void shouldAllowAccessToValidateEndpointWithoutJwt() throws Exception {
        // When: GET /api/auth/first-login/validate without JWT
        mockMvc.perform(get("/api/auth/first-login/validate")
                        .param("token", firstLoginToken))
                // Then: Should return 200 OK (public endpoint)
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should allow access to set-password endpoint without JWT")
    void shouldAllowAccessToSetPasswordEndpointWithoutJwt() throws Exception {
        // Given: Valid request
        FirstLoginSetPasswordRequest request = new FirstLoginSetPasswordRequest(
                firstLoginToken,
                NEW_PASSWORD,
                NEW_PASSWORD
        );

        // When: POST /api/auth/first-login/set-password without JWT
        mockMvc.perform(post("/api/auth/first-login/set-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                // Then: Should return 200 OK (public endpoint)
                .andExpect(status().isOk());
    }
}

package com.src.ap.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.src.ap.dto.auth.AuthResponse;
import com.src.ap.dto.auth.LoginRequest;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Logout Token Invalidation Tests")
class LogoutTokenInvalidationTest {

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

    private static final String TEST_USERNAME = "logoutuser";
    private static final String TEST_PASSWORD = "TestP@ssword123";
    private static final String TEST_EMAIL = "logoutuser@example.com";

    private User testUser;
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

        // Create test user
        testUser = User.builder()
                .username(TEST_USERNAME)
                .passwordHash(passwordEncoder.encode(TEST_PASSWORD))
                .email(TEST_EMAIL)
                .roles(Set.of(userRole))
                .enabled(true)
                .locked(false)
                .tokenVersion(0)
                .failedLoginAttempts(0)
                .build();

        userRepository.save(testUser);
    }

    // ========== LOGOUT KILLS ALL TOKENS TESTS ==========

    @Test
    @DisplayName("Should invalidate token after logout - token A becomes unusable")
    void shouldInvalidateTokenAfterLogout() throws Exception {
        // Step 1: Login → get token A
        LoginRequest loginRequest = new LoginRequest(TEST_USERNAME, TEST_PASSWORD);

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

        String tokenA = apiResponse.getData().getToken();

        // Step 2: Call POST /api/auth/logout with token A → get 200
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Logout successful. All sessions have been invalidated."));

        // Step 3: Use token A again → secured endpoint now returns 401 (token invalidated)
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should invalidate token after logout - cannot call logout twice with same token")
    void shouldNotAllowLogoutTwiceWithSameToken() throws Exception {
        // Step 1: Login → get token A
        LoginRequest loginRequest = new LoginRequest(TEST_USERNAME, TEST_PASSWORD);

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

        String tokenA = apiResponse.getData().getToken();

        // Step 2: Call POST /api/auth/logout with token A → get 200
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // Step 3: Try to call logout again with same token A → should return 401
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should allow login after logout and get new valid token")
    void shouldAllowLoginAfterLogoutAndGetNewValidToken() throws Exception {
        // Step 1: Login → get token A
        LoginRequest loginRequest = new LoginRequest(TEST_USERNAME, TEST_PASSWORD);

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

        String tokenA = apiResponse.getData().getToken();

        // Step 2: Call POST /api/auth/logout with token A
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // Step 3: Login again → get token B
        MvcResult loginResult2 = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String responseJson2 = loginResult2.getResponse().getContentAsString();
        ApiResponse<AuthResponse> apiResponse2 = objectMapper.readValue(
                responseJson2,
                objectMapper.getTypeFactory().constructParametricType(
                        ApiResponse.class,
                        AuthResponse.class
                )
        );

        String tokenB = apiResponse2.getData().getToken();

        // Step 4: Use token B → secured endpoint returns 200 (new token is valid)
        mockMvc.perform(post("/api/auth/change-password")
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "currentPassword": "TestP@ssword123",
                                    "newPassword": "NewP@ssword456",
                                    "confirmPassword": "NewP@ssword456"
                                }
                                """))
                .andExpect(status().isOk());

        // Step 5: Old token A should still be invalid
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should invalidate all previous tokens when logout is called")
    void shouldInvalidateAllPreviousTokensWhenLogoutIsCalled() throws Exception {
        // Step 1: Login → get token A
        LoginRequest loginRequest = new LoginRequest(TEST_USERNAME, TEST_PASSWORD);

        MvcResult loginResult1 = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String responseJson1 = loginResult1.getResponse().getContentAsString();
        ApiResponse<AuthResponse> apiResponse1 = objectMapper.readValue(
                responseJson1,
                objectMapper.getTypeFactory().constructParametricType(
                        ApiResponse.class,
                        AuthResponse.class
                )
        );

        String tokenA = apiResponse1.getData().getToken();

        // Step 2: Login again (without logging out) → get token B
        MvcResult loginResult2 = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String responseJson2 = loginResult2.getResponse().getContentAsString();
        ApiResponse<AuthResponse> apiResponse2 = objectMapper.readValue(
                responseJson2,
                objectMapper.getTypeFactory().constructParametricType(
                        ApiResponse.class,
                        AuthResponse.class
                )
        );

        String tokenB = apiResponse2.getData().getToken();

        // Step 3: Call logout with token B (this should invalidate all tokens)
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // Step 4: Both token A and token B should be invalidated
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should invalidate token immediately after logout - no grace period")
    void shouldInvalidateTokenImmediatelyAfterLogout() throws Exception {
        // Step 1: Login → get token A
        LoginRequest loginRequest = new LoginRequest(TEST_USERNAME, TEST_PASSWORD);

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

        String tokenA = apiResponse.getData().getToken();

        // Step 2: Verify token works before logout
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // Step 3: Immediately try to use the token again (no delay) → should fail
        mockMvc.perform(post("/api/auth/change-password")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "currentPassword": "TestP@ssword123",
                                    "newPassword": "NewP@ssword456",
                                    "confirmPassword": "NewP@ssword456"
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should require valid token to call logout endpoint")
    void shouldRequireValidTokenToCallLogoutEndpoint() throws Exception {
        // Given: No authentication (no token)

        // When: Try to call logout without token
        // Then: Should return 401
        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return success message on logout")
    void shouldReturnSuccessMessageOnLogout() throws Exception {
        // Step 1: Login → get token
        LoginRequest loginRequest = new LoginRequest(TEST_USERNAME, TEST_PASSWORD);

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

        String token = apiResponse.getData().getToken();

        // Step 2: Call logout and verify success message
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Logout successful. All sessions have been invalidated."))
                .andExpect(jsonPath("$.data").doesNotExist());
    }
}

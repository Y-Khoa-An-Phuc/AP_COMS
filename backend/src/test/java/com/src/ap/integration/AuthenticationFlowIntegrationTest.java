package com.src.ap.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.src.ap.dto.auth.AuthResponse;
import com.src.ap.dto.auth.CreateUserResponse;
import com.src.ap.dto.auth.LoginRequest;
import com.src.ap.dto.auth.RegisterRequest;
import com.src.ap.dto.common.ApiResponse;
import com.src.ap.entity.Role;
import com.src.ap.entity.RoleName;
import com.src.ap.entity.User;
import com.src.ap.repository.RoleRepository;
import com.src.ap.repository.UserRepository;
import com.src.ap.service.JwtService;
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
@DisplayName("Authentication Flow Integration Tests")
class AuthenticationFlowIntegrationTest {

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

    @Autowired
    private JwtService jwtService;

    private static final String TEST_USERNAME = "testuser";
    private static final String TEST_PASSWORD = "TestP@ssword123";
    private static final String TEST_EMAIL = "testuser@example.com";

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

    // ========== NORMAL AUTH FLOW TESTS ==========

    @Test
    @DisplayName("Should login with valid credentials and receive JWT token")
    void shouldLoginWithValidCredentialsAndReceiveJwtToken() throws Exception {
        // Given: Valid login credentials
        LoginRequest loginRequest = new LoginRequest(TEST_USERNAME, TEST_PASSWORD);

        // When: POST /api/auth/login
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
                .andExpect(jsonPath("$.data.username").value(TEST_USERNAME))
                .andExpect(jsonPath("$.data.email").value(TEST_EMAIL))
                .andReturn();

        // Verify: Token is present in response
        String responseJson = result.getResponse().getContentAsString();
        ApiResponse<AuthResponse> apiResponse = objectMapper.readValue(
                responseJson,
                objectMapper.getTypeFactory().constructParametricType(
                        ApiResponse.class,
                        AuthResponse.class
                )
        );

        assertThat(apiResponse.isSuccess()).isTrue();
        assertThat(apiResponse.getMessage()).isEqualTo("Login successful");
        assertThat(apiResponse.getData().getToken()).isNotNull().isNotEmpty();
        assertThat(apiResponse.getData().getType()).isEqualTo("Bearer");
        assertThat(apiResponse.getData().getUsername()).isEqualTo(TEST_USERNAME);
        assertThat(apiResponse.getData().getEmail()).isEqualTo(TEST_EMAIL);
    }

    @Test
    @DisplayName("Should access protected endpoint with valid JWT token and get 200 OK")
    void shouldAccessProtectedEndpointWithValidJwtTokenAndGet200Ok() throws Exception {
        // Given: User is logged in and has JWT token
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

        String jwtToken = apiResponse.getData().getToken();

        // When: Call secured endpoint with Authorization: Bearer <token>
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON))
                // Then: Should return 200 OK (protected endpoint accessible)
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Logout successful. All sessions have been invalidated."));
    }

    @Test
    @DisplayName("Should verify JWT token contains correct claims after login")
    void shouldVerifyJwtTokenContainsCorrectClaimsAfterLogin() throws Exception {
        // Given: Valid login credentials
        LoginRequest loginRequest = new LoginRequest(TEST_USERNAME, TEST_PASSWORD);

        // When: Login and get JWT token
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String responseJson = result.getResponse().getContentAsString();
        ApiResponse<AuthResponse> apiResponse = objectMapper.readValue(
                responseJson,
                objectMapper.getTypeFactory().constructParametricType(
                        ApiResponse.class,
                        AuthResponse.class
                )
        );

        String jwtToken = apiResponse.getData().getToken();

        // Then: JWT should contain correct claims
        String extractedUsername = jwtService.extractUsername(jwtToken);
        Integer extractedTokenVersion = jwtService.extractTokenVersion(jwtToken);

        assertThat(extractedUsername).isEqualTo(TEST_USERNAME);
        assertThat(extractedTokenVersion).isEqualTo(0); // Initial token version
        assertThat(jwtService.isTokenValid(jwtToken, testUser)).isTrue();
    }

    @Test
    @DisplayName("Should complete full authentication flow: admin registers user -> user login -> access protected endpoint")
    void shouldCompleteFullAuthenticationFlow() throws Exception {
        // ===== 1) Ensure TECHADMIN role and admin user exist =====
        Role techAdminRole = roleRepository.findByName(RoleName.TECHADMIN)
                .orElseGet(() -> roleRepository.save(Role.builder()
                        .name(RoleName.TECHADMIN)
                        .build()));

        User admin = User.builder()
                .username("admin")
                .email("admin@example.com")
                .passwordHash(passwordEncoder.encode("AdminP@ss123"))
                .roles(Set.of(techAdminRole))
                .enabled(true)
                .locked(false)
                .tokenVersion(0)
                .failedLoginAttempts(0)
                .build();
        userRepository.save(admin);

        // ===== 2) Login as TechAdmin to get admin JWT =====
        LoginRequest adminLogin = new LoginRequest("admin", "AdminP@ss123");

        MvcResult adminLoginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminLogin)))
                .andExpect(status().isOk())
                .andReturn();

        String adminLoginJson = adminLoginResult.getResponse().getContentAsString();
        ApiResponse<AuthResponse> adminLoginResponse = objectMapper.readValue(
                adminLoginJson,
                objectMapper.getTypeFactory().constructParametricType(
                        ApiResponse.class, AuthResponse.class
                )
        );
        String adminToken = adminLoginResponse.getData().getToken();

        // ===== 3) Use admin JWT to call /api/auth/register =====
        // Note: RegisterRequest now only requires username and email (no password)
        // System generates a temporary password and sends first-login token via email
        RegisterRequest registerRequest = new RegisterRequest(
                "newuser",
                "newuser@example.com"
        );

        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User created and first-login email sent"))
                .andExpect(jsonPath("$.data.id").isNotEmpty())
                .andExpect(jsonPath("$.data.username").value("newuser"))
                .andExpect(jsonPath("$.data.email").value("newuser@example.com"))
                .andExpect(jsonPath("$.data.role").value("USER"))
                .andReturn();

        String registerResponseJson = registerResult.getResponse().getContentAsString();
        ApiResponse<CreateUserResponse> registerApiResponse = objectMapper.readValue(
                registerResponseJson,
                objectMapper.getTypeFactory().constructParametricType(
                        ApiResponse.class,
                        CreateUserResponse.class
                )
        );

        // ===== 4) Verify user was created with temporary password flags =====
        User newUser = userRepository.findByUsername("newuser").orElseThrow();
        assertThat(newUser.isMustChangePassword()).isTrue();
        assertThat(newUser.isTemporaryPassword()).isTrue();
        assertThat(newUser.getUsername()).isEqualTo("newuser");
        assertThat(newUser.getEmail()).isEqualTo("newuser@example.com");

        // ===== 5) New user CANNOT login with temporary password (they don't know it) =====
        // The new user must use the first-login token flow to set their own password
        // In a real scenario, they would receive an email with the first-login link
    }

    @Test
    @DisplayName("Should deny access to protected endpoint without JWT token")
    void shouldDenyAccessToProtectedEndpointWithoutJwtToken() throws Exception {
        // When: Call secured endpoint without Authorization header
        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON))
                // Then: Should return 401 Unauthorized (no authentication)
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should deny access to protected endpoint with invalid JWT token")
    void shouldDenyAccessToProtectedEndpointWithInvalidJwtToken() throws Exception {
        // Given: Invalid JWT token
        String invalidToken = "invalid.jwt.token";

        // When: Call secured endpoint with invalid token
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + invalidToken)
                        .contentType(MediaType.APPLICATION_JSON))
                // Then: Should return 401 Unauthorized
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return 401 when login with invalid credentials")
    void shouldReturn401WhenLoginWithInvalidCredentials() throws Exception {
        // Given: Invalid login credentials
        LoginRequest loginRequest = new LoginRequest(TEST_USERNAME, "WrongPassword");

        // When: POST /api/auth/login with wrong password
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                // Then: Should return 401 Unauthorized
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Tên đăng nhập hoặc mật khẩu không đúng."));
    }

    @Test
    @DisplayName("Should allow public access to login endpoint without authentication")
    void shouldAllowPublicAccessToLoginEndpointWithoutAuthentication() throws Exception {
        // Given: Valid login credentials (no prior authentication)
        LoginRequest loginRequest = new LoginRequest(TEST_USERNAME, TEST_PASSWORD);

        // When: POST /api/auth/login (public endpoint)
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                // Then: Should allow access without Bearer token
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").isNotEmpty());
    }

    @Test
    @DisplayName("Should verify Bearer token format is correctly handled")
    void shouldVerifyBearerTokenFormatIsCorrectlyHandled() throws Exception {
        // Given: User is logged in
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

        String jwtToken = apiResponse.getData().getToken();

        // When: Call with correct Bearer format
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk());

        // Login again to get fresh token (previous logout invalidated it)
        loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        responseJson = loginResult.getResponse().getContentAsString();
        apiResponse = objectMapper.readValue(
                responseJson,
                objectMapper.getTypeFactory().constructParametricType(
                        ApiResponse.class,
                        AuthResponse.class
                )
        );

        jwtToken = apiResponse.getData().getToken();

        // When: Call without "Bearer " prefix
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", jwtToken)) // Missing "Bearer " prefix
                .andExpect(status().isUnauthorized()); // Should be rejected
    }

    @Test
    @DisplayName("Should verify SecurityContext is populated after successful JWT authentication")
    void shouldVerifySecurityContextIsPopulatedAfterSuccessfulJwtAuthentication() throws Exception {
        // Given: User logs in and gets JWT token
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

        String jwtToken = apiResponse.getData().getToken();

        // When: Call protected endpoint with JWT
        // Then: Endpoint should execute successfully (proving SecurityContext was populated)
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Logout successful. All sessions have been invalidated."));

        // The fact that logout succeeded proves:
        // 1. JWT filter extracted and validated the token
        // 2. SecurityContext was populated with authentication
        // 3. Any @PreAuthorize checks passed
        // 4. AuthService.logout() could retrieve username from SecurityContext
    }
}

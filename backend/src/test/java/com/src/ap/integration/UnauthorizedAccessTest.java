package com.src.ap.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Unauthorized Access Tests")
class UnauthorizedAccessTest {

    @Autowired
    private MockMvc mockMvc;

    // ========== NO TOKEN TESTS ==========

    @Test
    @DisplayName("Should return 401 when calling protected endpoint without Authorization header")
    void shouldReturn401WhenCallingProtectedEndpointWithoutAuthorizationHeader() throws Exception {
        // When: Call secured endpoint without Authorization header
        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON))
                // Then: Should return 401 Unauthorized
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return 401 when calling change-password endpoint without token")
    void shouldReturn401WhenCallingChangePasswordWithoutToken() throws Exception {
        // Given: A valid change password request body
        String changePasswordRequest = """
                {
                    "currentPassword": "OldP@ssword123",
                    "newPassword": "NewP@ssword456",
                    "confirmPassword": "NewP@ssword456"
                }
                """;

        // When: Call change-password endpoint without Authorization header
        mockMvc.perform(post("/api/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(changePasswordRequest))
                // Then: Should return 401 Unauthorized
                .andExpect(status().isUnauthorized());
    }

    // ========== INVALID/GARBAGE TOKEN TESTS ==========

    @Test
    @DisplayName("Should return 401 when calling protected endpoint with garbage token")
    void shouldReturn401WhenCallingProtectedEndpointWithGarbageToken() throws Exception {
        // Given: A completely invalid/garbage token
        String garbageToken = "this-is-not-a-valid-jwt-token";

        // When: Call secured endpoint with garbage token
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + garbageToken)
                        .contentType(MediaType.APPLICATION_JSON))
                // Then: Should return 401 Unauthorized
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return 401 when calling protected endpoint with malformed JWT")
    void shouldReturn401WhenCallingProtectedEndpointWithMalformedJWT() throws Exception {
        // Given: A malformed JWT (missing parts)
        String malformedToken = "eyJhbGciOiJIUzI1NiJ9.malformed";

        // When: Call secured endpoint with malformed token
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + malformedToken)
                        .contentType(MediaType.APPLICATION_JSON))
                // Then: Should return 401 Unauthorized
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return 401 when calling protected endpoint with empty token")
    void shouldReturn401WhenCallingProtectedEndpointWithEmptyToken() throws Exception {
        // Given: An empty token
        String emptyToken = "";

        // When: Call secured endpoint with empty token
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + emptyToken)
                        .contentType(MediaType.APPLICATION_JSON))
                // Then: Should return 401 Unauthorized
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return 401 when calling protected endpoint with random string token")
    void shouldReturn401WhenCallingProtectedEndpointWithRandomStringToken() throws Exception {
        // Given: A random string that looks nothing like a JWT
        String randomToken = "abcdefghijklmnopqrstuvwxyz123456789";

        // When: Call secured endpoint with random token
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + randomToken)
                        .contentType(MediaType.APPLICATION_JSON))
                // Then: Should return 401 Unauthorized
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return 401 when Authorization header missing 'Bearer' prefix")
    void shouldReturn401WhenAuthorizationHeaderMissingBearerPrefix() throws Exception {
        // Given: A token without "Bearer " prefix
        String token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ0ZXN0In0.test";

        // When: Call secured endpoint without "Bearer " prefix
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", token) // Missing "Bearer " prefix
                        .contentType(MediaType.APPLICATION_JSON))
                // Then: Should return 401 Unauthorized
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return 401 when Authorization header has wrong scheme")
    void shouldReturn401WhenAuthorizationHeaderHasWrongScheme() throws Exception {
        // Given: A token with wrong scheme (Basic instead of Bearer)
        String token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ0ZXN0In0.test";

        // When: Call secured endpoint with wrong scheme
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Basic " + token) // Wrong scheme
                        .contentType(MediaType.APPLICATION_JSON))
                // Then: Should return 401 Unauthorized
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return 401 when calling change-password with garbage token")
    void shouldReturn401WhenCallingChangePasswordWithGarbageToken() throws Exception {
        // Given: A garbage token and valid request body
        String garbageToken = "garbage-token-12345";
        String changePasswordRequest = """
                {
                    "currentPassword": "OldP@ssword123",
                    "newPassword": "NewP@ssword456",
                    "confirmPassword": "NewP@ssword456"
                }
                """;

        // When: Call change-password endpoint with garbage token
        mockMvc.perform(post("/api/auth/change-password")
                        .header("Authorization", "Bearer " + garbageToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(changePasswordRequest))
                // Then: Should return 401 Unauthorized
                .andExpect(status().isUnauthorized());
    }

    // ========== EDGE CASES ==========

    @Test
    @DisplayName("Should return 401 when Authorization header contains only spaces")
    void shouldReturn401WhenAuthorizationHeaderContainsOnlySpaces() throws Exception {
        // Given: Authorization header with only spaces
        String spacesToken = "   ";

        // When: Call secured endpoint
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", spacesToken)
                        .contentType(MediaType.APPLICATION_JSON))
                // Then: Should return 401 Unauthorized
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return 401 when Authorization header is 'Bearer' without token")
    void shouldReturn401WhenAuthorizationHeaderIsBearerWithoutToken() throws Exception {
        // When: Call secured endpoint with just "Bearer" and no token
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer")
                        .contentType(MediaType.APPLICATION_JSON))
                // Then: Should return 401 Unauthorized
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return 401 when Authorization header is 'Bearer ' with only space")
    void shouldReturn401WhenAuthorizationHeaderIsBearerWithOnlySpace() throws Exception {
        // When: Call secured endpoint with "Bearer " followed by space
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer ")
                        .contentType(MediaType.APPLICATION_JSON))
                // Then: Should return 401 Unauthorized
                .andExpect(status().isUnauthorized());
    }

    // ========== VERIFY PUBLIC ENDPOINTS ARE ACCESSIBLE ==========

    @Test
    @DisplayName("Should allow access to login endpoint without token (public endpoint)")
    void shouldAllowAccessToLoginEndpointWithoutToken() throws Exception {
        // Given: A login request
        String loginRequest = """
        {
            "username": "testuser",
            "password": "TestP@ssword123"
        }
        """;

        // When: Call login endpoint without token
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequest))
                // Then: Should NOT return 403
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assertNotEquals(403, status,
                            "Login endpoint should be accessible without JWT (must not return 403)");
                });
    }

    @Test
    @DisplayName("Should return 401 when calling register endpoint without TECHADMIN token")
    void shouldReturn401WhenCallingRegisterEndpointWithoutToken() throws Exception {
        // Given: A registration request
        String registerRequest = """
            {
                "username": "newuser",
                "password": "NewP@ssword123",
                "email": "newuser@example.com"
            }
            """;

        // When: Call register endpoint without token (requires TECHADMIN role)
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerRequest))
                // Then: Should return 401 Unauthorized (authentication required)
                .andExpect(status().isUnauthorized());
    }
}

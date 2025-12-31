package com.src.ap.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class SecurityAccessIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /api/auth/login should be publicly accessible (no auth required)")
    void getLoginShouldBePublic() throws Exception {
        mockMvc.perform(get("/api/auth/login")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    // Security-wise: must NOT be 401 or 403
                    assertNotEquals(401, status, "GET /api/auth/login should not be unauthorized");
                    assertNotEquals(403, status, "GET /api/auth/login should not be forbidden");
                });
    }
}

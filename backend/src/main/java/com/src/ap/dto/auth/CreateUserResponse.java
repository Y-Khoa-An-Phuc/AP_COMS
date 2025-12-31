package com.src.ap.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO returned when an admin creates a new user.
 * Contains basic user information for confirmation.
 * Does NOT include authentication tokens - users must complete first-login flow.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateUserResponse {

    private Long id;
    private String username;
    private String email;
    private String role;
}

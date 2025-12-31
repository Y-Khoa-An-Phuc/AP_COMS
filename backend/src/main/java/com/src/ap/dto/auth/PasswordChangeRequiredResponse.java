package com.src.ap.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordChangeRequiredResponse {
    private boolean mustChangePassword;
    private boolean firstLogin;
    private String message;
}
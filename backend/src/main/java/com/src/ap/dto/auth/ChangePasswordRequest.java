package com.src.ap.dto.auth;

import com.src.ap.validation.ValidPassword;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChangePasswordRequest {

    @NotBlank(message = "Current password is required")
    private String currentPassword;

    @NotBlank(message = "New password is required")
    @ValidPassword
    private String newPassword;

    @NotBlank(message = "Password confirmation is required")
    private String confirmPassword;
}
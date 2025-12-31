package com.src.ap.controller;

import com.src.ap.dto.auth.AuthResponse;
import com.src.ap.dto.auth.ChangePasswordRequest;
import com.src.ap.dto.auth.ChangeTemporaryPasswordRequest;
import com.src.ap.dto.auth.CreateUserResponse;
import com.src.ap.dto.auth.FirstLoginSetPasswordRequest;
import com.src.ap.dto.auth.FirstLoginValidateResponse;
import com.src.ap.dto.auth.LoginRequest;
import com.src.ap.dto.auth.RegisterRequest;
import com.src.ap.dto.common.ApiResponse;
import com.src.ap.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<CreateUserResponse>> register(@Valid @RequestBody RegisterRequest request) {
        CreateUserResponse response = authService.register(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("User created and first-login email sent", response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully", null));
    }

    /**
     * @deprecated This endpoint is deprecated in favor of the token-based first-login flow.
     * Use /api/auth/first-login/set-password instead.
     * This endpoint will be removed in a future version.
     */
    @Deprecated
    @PostMapping("/change-temporary-password")
    public ResponseEntity<ApiResponse<Void>> changeTemporaryPassword(@Valid @RequestBody ChangeTemporaryPasswordRequest request) {
        authService.changeTemporaryPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully. Please login again with your new password.", null));
    }

    @GetMapping("/first-login/validate")
    public ResponseEntity<ApiResponse<FirstLoginValidateResponse>> validateFirstLoginToken(@RequestParam String token) {
        FirstLoginValidateResponse response = authService.validateFirstLoginToken(token);
        return ResponseEntity.ok(ApiResponse.success("Token is valid", response));
    }

    @PostMapping("/first-login/set-password")
    public ResponseEntity<ApiResponse<AuthResponse>> setPasswordWithFirstLoginToken(@Valid @RequestBody FirstLoginSetPasswordRequest request) {
        AuthResponse response = authService.setPasswordWithFirstLoginToken(request);
        return ResponseEntity.ok(ApiResponse.success("Password set successfully. You are now logged in.", response));
    }

    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> logout() {
        authService.logout();
        return ResponseEntity.ok(ApiResponse.success("Logout successful. All sessions have been invalidated.", null));
    }
}
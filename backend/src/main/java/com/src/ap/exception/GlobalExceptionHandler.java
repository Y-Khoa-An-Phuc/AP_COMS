package com.src.ap.exception;

import com.src.ap.dto.auth.PasswordChangeRequiredResponse;
import com.src.ap.dto.common.ApiResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for the application.
 * Provides consistent JSON structure for all error responses.
 *
 * <p><b>Authentication & Authorization Error Responses:</b></p>
 * <ul>
 *   <li><b>401 Unauthorized:</b> Invalid credentials or authentication failed</li>
 *   <li><b>403 Forbidden:</b> Authenticated but insufficient permissions, or temporary password required</li>
 *   <li><b>400 Bad Request:</b> Validation errors or invalid input</li>
 *   <li><b>409 Conflict:</b> Resource already exists (e.g., duplicate username)</li>
 * </ul>
 *
 * <p>All responses follow the ApiResponse structure with consistent fields:
 * success, message, data, timestamp</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFoundException(ResourceNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicateResourceException(DuplicateResourceException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadRequestException(BadRequestException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnauthorizedException(UnauthorizedException ex) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Handles PasswordChangeRequiredException - thrown when user attempts login with temporary password.
     *
     * <p><b>HTTP Status:</b> 403 Forbidden</p>
     * <p><b>Response Structure:</b></p>
     * <pre>
     * {
     *   "success": false,
     *   "message": "User must change password before continuing.",
     *   "data": {
     *     "mustChangePassword": true,
     *     "firstLogin": true,
     *     "message": "User must change password before continuing."
     *   },
     *   "timestamp": "2025-12-04T10:30:00"
     * }
     * </pre>
     *
     * @param ex the PasswordChangeRequiredException
     * @return 403 response with password change requirement details
     */
    @ExceptionHandler(PasswordChangeRequiredException.class)
    public ResponseEntity<ApiResponse<PasswordChangeRequiredResponse>> handlePasswordChangeRequiredException(PasswordChangeRequiredException ex) {
        PasswordChangeRequiredResponse data = PasswordChangeRequiredResponse.builder()
                .mustChangePassword(ex.isMustChangePassword())
                .firstLogin(ex.isFirstLogin())
                .message(ex.getMessage())
                .build();

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.<PasswordChangeRequiredResponse>builder()
                        .success(false)
                        .message(ex.getMessage())
                        .data(data)
                        .build());
    }

    /**
     * Handles BadCredentialsException - thrown when authentication fails due to invalid credentials.
     *
     * <p><b>HTTP Status:</b> 401 Unauthorized</p>
     * <p><b>Response:</b> Generic error message (does not reveal if username or password is incorrect)</p>
     *
     * @param ex the BadCredentialsException
     * @return 401 response with generic error message
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentialsException(BadCredentialsException ex) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Tên đăng nhập hoặc mật khẩu không đúng."));
    }

    /**
     * Handles DisabledException - thrown when user account is disabled.
     *
     * <p><b>HTTP Status:</b> 403 Forbidden</p>
     * <p><b>Response:</b> Account disabled message</p>
     *
     * @param ex the DisabledException
     * @return 403 response with account disabled message
     */
    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ApiResponse<Void>> handleDisabledException(DisabledException ex) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("Tài khoản đã bị vô hiệu hóa. Vui lòng liên hệ quản trị viên."));
    }

    /**
     * Handles LockedException - thrown when user account is locked.
     *
     * <p><b>HTTP Status:</b> 403 Forbidden</p>
     * <p><b>Response:</b> Account locked message</p>
     *
     * @param ex the LockedException
     * @return 403 response with account locked message
     */
    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ApiResponse<Void>> handleLockedException(LockedException ex) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("Tài khoản đã bị khóa do quá nhiều lần đăng nhập thất bại. Vui lòng thử lại sau."));
    }

    /**
     * Handles AccessDeniedException - thrown when user lacks required permissions.
     * Handles AccessDeniedException - thrown when user lacks required permissions.
     *
     * <p><b>HTTP Status:</b> 403 Forbidden</p>
     *
     * @param ex the AccessDeniedException
     * @return 403 response with access denied message
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(AccessDeniedException ex) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("Access denied"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.<Map<String, String>>builder()
                        .success(false)
                        .message("Validation failed")
                        .data(errors)
                        .build());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolationException(DataIntegrityViolationException ex) {
        String message = "Data integrity violation.";
        String rootMessage = ex.getMostSpecificCause() != null ? ex.getMostSpecificCause().getMessage() : ex.getMessage();
        if (rootMessage != null) {
            String lower = rootMessage.toLowerCase();
            if (rootMessage.contains("UK149od0cl28ed0rs7wxgrjbch7") || lower.contains("occupations")) {
                message = "Occupation name already exists.";
            } else if (lower.contains("unique") || lower.contains("duplicate")) {
                message = "Duplicate value already exists.";
            }
        }

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("An unexpected error occurred: " + ex.getMessage()));
    }
}

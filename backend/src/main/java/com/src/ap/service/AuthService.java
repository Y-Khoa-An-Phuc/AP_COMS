package com.src.ap.service;

import com.src.ap.dto.auth.AuthResponse;
import com.src.ap.dto.auth.ChangePasswordRequest;
import com.src.ap.dto.auth.ChangeTemporaryPasswordRequest;
import com.src.ap.dto.auth.CreateUserResponse;
import com.src.ap.dto.auth.FirstLoginSetPasswordRequest;
import com.src.ap.dto.auth.FirstLoginValidateResponse;
import com.src.ap.dto.auth.LoginRequest;
import com.src.ap.dto.auth.RegisterRequest;
import com.src.ap.entity.Role;
import com.src.ap.entity.RoleName;
import com.src.ap.entity.User;
import com.src.ap.exception.BadRequestException;
import com.src.ap.exception.DuplicateResourceException;
import com.src.ap.exception.ResourceNotFoundException;
import com.src.ap.exception.UnauthorizedException;
import com.src.ap.entity.OneTimeToken;
import com.src.ap.entity.TokenType;
import com.src.ap.repository.RoleRepository;
import com.src.ap.repository.UserRepository;
import com.src.ap.util.PasswordGenerator;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final OneTimeTokenService oneTimeTokenService;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Value("${app.frontend.base-url}")
    private String frontendBaseUrl;

    @Transactional
    public CreateUserResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("User", "username", request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("User", "email", request.getEmail());
        }

        Role userRole = roleRepository.findByName(RoleName.USER)
                .orElseThrow(() -> new RuntimeException("USER role not found"));

        // Generate a secure temporary password
        String temporaryPassword = PasswordGenerator.generateTemporaryPassword();

        User user = User.builder()
                .username(request.getUsername())
                .passwordHash(passwordEncoder.encode(temporaryPassword))
                .email(request.getEmail())
                .roles(Set.of(userRole))
                .enabled(true)
                .locked(false)
                .mustChangePassword(true)
                .temporaryPassword(true)
                .build();

        userRepository.save(user);

        // Generate and save a one-time token for first login
        OneTimeToken firstLoginToken = oneTimeTokenService.createFirstLoginToken(user);

        // Compose the first login link
        String firstLoginLink = frontendBaseUrl + "/first-login?token=" + firstLoginToken.getToken();

        // Send first login email with the link (user will set their own password)
        emailService.sendFirstLoginEmail(user, firstLoginLink);
        log.info("User {} created with temporary password. First login email sent to {}", user.getUsername(), user.getEmail());

        // Return user information for admin confirmation
        return CreateUserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(getRolesAsString(user))
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                request.getUsername(),
                request.getPassword()
        );

        // Set authentication details (including IP address) from current HTTP request
        HttpServletRequest httpRequest = getCurrentHttpRequest();
        if (httpRequest != null) {
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(httpRequest));
        }

        authenticationManager.authenticate(authToken);

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow();

        // Note: mustChangePassword check has been removed. Users with temporary passwords
        // should use the first-login token flow instead.

        String token = jwtService.generateToken(user);

        return AuthResponse.builder()
                .token(token)
                .type("Bearer")
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(getRolesAsString(user))
                .build();
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            log.warn("Failed password change attempt for user: {}", username);
            throw new UnauthorizedException("Current password is incorrect");
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("New password and confirmation do not match");
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new BadRequestException("New password must be different from current password");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        // Clear temporary password flags if they were set
        user.setMustChangePassword(false);
        user.setTemporaryPassword(false);
        // Increment tokenVersion to invalidate all existing sessions for security
        user.setTokenVersion(user.getTokenVersion() + 1);
        userRepository.save(user);

        log.info("Password successfully changed for user: {}, tokenVersion incremented", username);
    }

    /**
     * Changes a temporary password for a user who hasn't been issued a JWT token yet.
     * This method authenticates the user via username and temporary password,
     * then allows them to set a new password. User must login again after successful change.
     *
     * @deprecated This method is deprecated in favor of the token-based first-login flow.
     * Use setPasswordWithFirstLoginToken() instead. This method will be removed in a future version.
     *
     * @param request Contains username, current temporary password, new password, and confirmation
     * @throws BadRequestException if password has already been changed or validation fails
     * @throws UnauthorizedException if current password is incorrect
     */
    @Deprecated
    @Transactional
    public void changeTemporaryPassword(ChangeTemporaryPasswordRequest request) {
        // Authenticate user with username and current password
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                request.getUsername(),
                request.getCurrentPassword()
        );

        HttpServletRequest httpRequest = getCurrentHttpRequest();
        if (httpRequest != null) {
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(httpRequest));
        }

        // This will throw BadCredentialsException if authentication fails (caught by global handler)
        authenticationManager.authenticate(authToken);

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", request.getUsername()));

        // MUST have BOTH flags set to true - only allow this flow for temporary passwords
        if (!user.isMustChangePassword() || !user.isTemporaryPassword()) {
            throw new BadRequestException("Password has already been changed");
        }

        // Validate new password and confirmation match
        if (!request.getNewPassword().equals(request.getConfirmNewPassword())) {
            throw new BadRequestException("New password and confirmation do not match");
        }

        // Ensure new password is different from temporary password
        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new BadRequestException("New password must be different from temporary password");
        }

        // Update password and clear temporary password flags
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setMustChangePassword(false);
        user.setTemporaryPassword(false);
        // Increment tokenVersion to invalidate any tokens that might have been issued
        user.setTokenVersion(user.getTokenVersion() + 1);
        userRepository.save(user);

        log.info("Temporary password successfully changed for user: {}. User must login again.", user.getUsername());
    }

    /**
     * Logs out the user by incrementing their tokenVersion
     * This invalidates ALL tokens issued before this logout (global logout)
     */
    @Transactional
    public void logout() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        user.setTokenVersion(user.getTokenVersion() + 1);
        userRepository.save(user);

        log.info("User {} logged out successfully, tokenVersion incremented to {}", username, user.getTokenVersion());
    }

    private String getRolesAsString(User user) {
        return user.getRoles().stream()
                .map(role -> role.getName().name())
                .collect(Collectors.joining(", "));
    }

    /**
     * Retrieves the current HTTP request from the request context.
     * This is used to extract client IP address for security logging.
     *
     * @return the current HttpServletRequest or null if not in a web request context
     */
    private HttpServletRequest getCurrentHttpRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }

    /**
     * Validates a first login token and returns user information if valid.
     *
     * @param token the first login token to validate
     * @return FirstLoginValidateResponse with user information
     * @throws BadRequestException if token is invalid or already used
     */
    public FirstLoginValidateResponse validateFirstLoginToken(String token) {
        // Validate token using the service
        OneTimeToken oneTimeToken = oneTimeTokenService.validateToken(token, TokenType.FIRST_LOGIN);

        User user = oneTimeToken.getUser();

        // Token is only valid if user still has temporary password flags
        if (!user.isMustChangePassword() || !user.isTemporaryPassword()) {
            throw new BadRequestException("Token has already been used or is no longer valid");
        }

        return FirstLoginValidateResponse.builder()
                .valid(true)
                .username(user.getUsername())
                .email(user.getEmail())
                .build();
    }

    /**
     * Sets a new password for a user using their first login token.
     * This is a one-time operation that consumes the token and automatically logs the user in.
     *
     * @param request Contains token, new password, and confirmation
     * @return AuthResponse with JWT token for immediate login
     * @throws BadRequestException if token is invalid, passwords don't match, or validation fails
     */
    @Transactional
    public AuthResponse setPasswordWithFirstLoginToken(FirstLoginSetPasswordRequest request) {
        // Validate token using the service
        OneTimeToken oneTimeToken = oneTimeTokenService.validateToken(request.getToken(), TokenType.FIRST_LOGIN);

        User user = oneTimeToken.getUser();

        // Token is only valid if user still has temporary password flags
        if (!user.isMustChangePassword() || !user.isTemporaryPassword()) {
            throw new BadRequestException("Token has already been used or is no longer valid");
        }

        // Validate new password and confirmation match
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("New password and confirmation do not match");
        }

        // Ensure new password is different from temporary password
        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new BadRequestException("New password must be different from temporary password");
        }

        // Update password, clear flags
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setMustChangePassword(false);
        user.setTemporaryPassword(false);
        user.setTokenVersion(user.getTokenVersion() + 1);
        userRepository.save(user);

        // Mark token as used (consume it)
        oneTimeTokenService.markTokenAsUsed(oneTimeToken);

        log.info("Password successfully set for user {} via first login token. Auto-login JWT issued.", user.getUsername());

        // Generate JWT token for immediate auto-login
        String token = jwtService.generateToken(user);

        return AuthResponse.builder()
                .token(token)
                .type("Bearer")
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(getRolesAsString(user))
                .build();
    }
}

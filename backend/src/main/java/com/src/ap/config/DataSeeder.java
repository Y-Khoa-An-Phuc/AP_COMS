package com.src.ap.config;

import com.src.ap.entity.OneTimeToken;
import com.src.ap.entity.Role;
import com.src.ap.entity.RoleName;
import com.src.ap.entity.User;
import com.src.ap.repository.RoleRepository;
import com.src.ap.repository.UserRepository;
import com.src.ap.service.EmailService;
import com.src.ap.service.OneTimeTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
@org.springframework.context.annotation.Profile("!test")
public class DataSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final OneTimeTokenService oneTimeTokenService;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Value("${ADMIN_USERNAME}")
    private String superAdminUsername;

    @Value("${ADMIN_PASSWORD}")
    private String superAdminPassword;

    @Value("${ADMIN_EMAIL}")
    private String superAdminEmail;

    @Value("${app.frontend.base-url}")
    private String frontendBaseUrl;

    @Override
    public void run(String... args) {
        seedRoles();
        seedSuperAdminUser();
    }

    private void seedRoles() {
        for (RoleName roleName : RoleName.values()) {
            if (roleRepository.findByName(roleName).isEmpty()) {
                Role role = new Role(roleName);
                roleRepository.save(role);
                log.info("Created role: {}", roleName);
            }
        }
    }

    private void seedSuperAdminUser() {
        if (userRepository.existsByUsername(superAdminUsername)) {
            log.info("Super Admin user already exists");
            return;
        }

        Role techAdminRole = roleRepository.findByName(RoleName.TECHADMIN)
                .orElseThrow(() -> new RuntimeException("TECHADMIN role not found"));

        User superAdmin = User.builder()
                .username(superAdminUsername)
                .passwordHash(passwordEncoder.encode(superAdminPassword))
                .email(superAdminEmail)
                .enabled(true)  // Account is enabled, but password change is required
                .mustChangePassword(true)
                .temporaryPassword(true)  // Mark as temporary password
                .locked(false)
                .roles(Set.of(techAdminRole))
                .build();

        userRepository.save(superAdmin);

        // Generate and save a one-time token for first login
        OneTimeToken firstLoginToken = oneTimeTokenService.createFirstLoginToken(superAdmin);

        // Compose the first login link
        String firstLoginLink = frontendBaseUrl + "/first-login?token=" + firstLoginToken.getToken();

        // Send first login email
        emailService.sendFirstLoginEmail(superAdmin, firstLoginLink);

        log.info("Created initial super Admin user: {}", superAdminUsername);
        log.warn("IMPORTANT: Change the default password immediately!");
        log.info("First login email sent to: {}", superAdminEmail);
    }
}

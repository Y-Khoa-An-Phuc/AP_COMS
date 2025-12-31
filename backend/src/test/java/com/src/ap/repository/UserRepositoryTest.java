package com.src.ap.repository;

import com.src.ap.entity.Role;
import com.src.ap.entity.RoleName;
import com.src.ap.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("UserRepository Tests")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    private Role userRole;

    @BeforeEach
    void setUp() {
        // Create and persist a role for testing
        userRole = roleRepository.save(new Role(RoleName.USER));
    }

    @Test
    @DisplayName("Should save a new user successfully")
    void testSaveUser() {
        // Arrange
        User user = User.builder()
                .username("testuser")
                .passwordHash("$2a$10$hashedPassword")
                .email("testuser@example.com")
                .enabled(true)
                .locked(false)
                .roles(Set.of(userRole))
                .build();

        // Act
        User savedUser = userRepository.save(user);

        // Assert
        assertThat(savedUser).isNotNull();
        assertThat(savedUser.getId()).isNotNull();
        assertThat(savedUser.getUsername()).isEqualTo("testuser");
        assertThat(savedUser.getEmail()).isEqualTo("testuser@example.com");
        assertThat(savedUser.getPasswordHash()).isEqualTo("$2a$10$hashedPassword");
        assertThat(savedUser.isEnabled()).isTrue();
        assertThat(savedUser.isLocked()).isFalse();
        assertThat(savedUser.getRoles()).hasSize(1);
        assertThat(savedUser.getCreatedAt()).isNotNull();
        assertThat(savedUser.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should find user by username when user exists")
    void testFindByUsername_WhenUserExists() {
        // Arrange
        User user = User.builder()
                .username("john.doe")
                .passwordHash("$2a$10$hashedPassword")
                .email("john.doe@example.com")
                .enabled(true)
                .locked(false)
                .roles(Set.of(userRole))
                .build();
        userRepository.save(user);

        // Act
        Optional<User> foundUser = userRepository.findByUsername("john.doe");

        // Assert
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getUsername()).isEqualTo("john.doe");
        assertThat(foundUser.get().getEmail()).isEqualTo("john.doe@example.com");
    }

    @Test
    @DisplayName("Should return empty Optional when user does not exist")
    void testFindByUsername_WhenUserDoesNotExist() {
        // Act
        Optional<User> foundUser = userRepository.findByUsername("nonexistent");

        // Assert
        assertThat(foundUser).isEmpty();
    }

    @Test
    @DisplayName("Should return true when user exists by username")
    void testExistsByUsername_WhenUserExists() {
        // Arrange
        User user = User.builder()
                .username("existinguser")
                .passwordHash("$2a$10$hashedPassword")
                .email("existinguser@example.com")
                .enabled(true)
                .locked(false)
                .roles(Set.of(userRole))
                .build();
        userRepository.save(user);

        // Act
        boolean exists = userRepository.existsByUsername("existinguser");

        // Assert
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("Should return false when user does not exist by username")
    void testExistsByUsername_WhenUserDoesNotExist() {
        // Act
        boolean exists = userRepository.existsByUsername("nonexistentuser");

        // Assert
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("Should return true when user exists by email")
    void testExistsByEmail_WhenUserExists() {
        // Arrange
        User user = User.builder()
                .username("emailtest")
                .passwordHash("$2a$10$hashedPassword")
                .email("emailtest@example.com")
                .enabled(true)
                .locked(false)
                .roles(Set.of(userRole))
                .build();
        userRepository.save(user);

        // Act
        boolean exists = userRepository.existsByEmail("emailtest@example.com");

        // Assert
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("Should return false when user does not exist by email")
    void testExistsByEmail_WhenUserDoesNotExist() {
        // Act
        boolean exists = userRepository.existsByEmail("nonexistent@example.com");

        // Assert
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("Should persist user with multiple roles")
    void testSaveUser_WithMultipleRoles() {
        // Arrange
        Role adminRole = roleRepository.save(new Role(RoleName.TECHADMIN));

        User user = User.builder()
                .username("adminuser")
                .passwordHash("$2a$10$hashedPassword")
                .email("admin@example.com")
                .enabled(true)
                .locked(false)
                .roles(Set.of(userRole, adminRole))
                .build();

        // Act
        User savedUser = userRepository.save(user);

        // Assert
        assertThat(savedUser.getRoles()).hasSize(2);
        assertThat(savedUser.getRoles()).extracting(Role::getName)
                .containsExactlyInAnyOrder(RoleName.USER, RoleName.TECHADMIN);
    }

    @Test
    @DisplayName("Should update user successfully")
    void testUpdateUser() {
        // Arrange
        User user = User.builder()
                .username("updatetest")
                .passwordHash("$2a$10$hashedPassword")
                .email("updatetest@example.com")
                .enabled(true)
                .locked(false)
                .roles(Set.of(userRole))
                .build();
        User savedUser = userRepository.save(user);

        // Act
        savedUser.setEmail("newemail@example.com");
        savedUser.setLocked(true);
        User updatedUser = userRepository.save(savedUser);

        // Assert
        assertThat(updatedUser.getEmail()).isEqualTo("newemail@example.com");
        assertThat(updatedUser.isLocked()).isTrue();
        assertThat(updatedUser.getUpdatedAt()).isNotNull();
    }

    // ========== TEMPORARY PASSWORD FIELD TESTS ==========

    @Test
    @DisplayName("Should persist mustChangePassword field with default value false")
    void testPersistMustChangePasswordFieldWithDefaultValueFalse() {
        // Arrange
        User user = User.builder()
                .username("defaultflagsuser")
                .passwordHash("$2a$10$hashedPassword")
                .email("defaultflags@example.com")
                .enabled(true)
                .locked(false)
                .roles(Set.of(userRole))
                .build();

        // Act
        User savedUser = userRepository.save(user);

        // Assert
        assertThat(savedUser.isMustChangePassword()).isFalse();
        assertThat(savedUser.isTemporaryPassword()).isFalse();
    }

    @Test
    @DisplayName("Should persist mustChangePassword field when set to true")
    void testPersistMustChangePasswordFieldWhenSetToTrue() {
        // Arrange
        User user = User.builder()
                .username("temppassuser")
                .passwordHash("$2a$10$hashedPassword")
                .email("temppass@example.com")
                .enabled(true)
                .locked(false)
                .mustChangePassword(true)
                .temporaryPassword(true)
                .roles(Set.of(userRole))
                .build();

        // Act
        User savedUser = userRepository.save(user);
        User retrievedUser = userRepository.findByUsername("temppassuser").orElseThrow();

        // Assert
        assertThat(savedUser.isMustChangePassword()).isTrue();
        assertThat(savedUser.isTemporaryPassword()).isTrue();
        assertThat(retrievedUser.isMustChangePassword()).isTrue();
        assertThat(retrievedUser.isTemporaryPassword()).isTrue();
    }

    @Test
    @DisplayName("Should update mustChangePassword and temporaryPassword fields")
    void testUpdateMustChangePasswordAndTemporaryPasswordFields() {
        // Arrange
        User user = User.builder()
                .username("updateflagsuser")
                .passwordHash("$2a$10$hashedPassword")
                .email("updateflags@example.com")
                .enabled(true)
                .locked(false)
                .mustChangePassword(true)
                .temporaryPassword(true)
                .roles(Set.of(userRole))
                .build();
        User savedUser = userRepository.save(user);

        // Act - Clear the flags (simulating password change)
        savedUser.setMustChangePassword(false);
        savedUser.setTemporaryPassword(false);
        User updatedUser = userRepository.save(savedUser);

        // Assert
        assertThat(updatedUser.isMustChangePassword()).isFalse();
        assertThat(updatedUser.isTemporaryPassword()).isFalse();

        // Verify persistence by retrieving from DB
        User retrievedUser = userRepository.findByUsername("updateflagsuser").orElseThrow();
        assertThat(retrievedUser.isMustChangePassword()).isFalse();
        assertThat(retrievedUser.isTemporaryPassword()).isFalse();
    }

    @Test
    @DisplayName("Should persist user with temporary password flags independently")
    void testPersistUserWithTemporaryPasswordFlagsIndependently() {
        // Arrange - User with only mustChangePassword=true
        User user1 = User.builder()
                .username("mustchangeonly")
                .passwordHash("$2a$10$hashedPassword")
                .email("mustchangeonly@example.com")
                .enabled(true)
                .locked(false)
                .mustChangePassword(true)
                .temporaryPassword(false)
                .roles(Set.of(userRole))
                .build();

        // Arrange - User with only temporaryPassword=true
        User user2 = User.builder()
                .username("temponly")
                .passwordHash("$2a$10$hashedPassword")
                .email("temponly@example.com")
                .enabled(true)
                .locked(false)
                .mustChangePassword(false)
                .temporaryPassword(true)
                .roles(Set.of(userRole))
                .build();

        // Act
        User savedUser1 = userRepository.save(user1);
        User savedUser2 = userRepository.save(user2);

        // Assert
        assertThat(savedUser1.isMustChangePassword()).isTrue();
        assertThat(savedUser1.isTemporaryPassword()).isFalse();

        assertThat(savedUser2.isMustChangePassword()).isFalse();
        assertThat(savedUser2.isTemporaryPassword()).isTrue();
    }

    @Test
    @DisplayName("Should query users by temporary password flags")
    void testQueryUsersByTemporaryPasswordFlags() {
        // Arrange - Create users with different flag combinations
        User normalUser = User.builder()
                .username("normaluser")
                .passwordHash("$2a$10$hashedPassword")
                .email("normal@example.com")
                .enabled(true)
                .locked(false)
                .mustChangePassword(false)
                .temporaryPassword(false)
                .roles(Set.of(userRole))
                .build();

        User tempPasswordUser = User.builder()
                .username("tempuser")
                .passwordHash("$2a$10$hashedPassword")
                .email("temp@example.com")
                .enabled(true)
                .locked(false)
                .mustChangePassword(true)
                .temporaryPassword(true)
                .roles(Set.of(userRole))
                .build();

        userRepository.save(normalUser);
        userRepository.save(tempPasswordUser);

        // Act
        User retrievedNormalUser = userRepository.findByUsername("normaluser").orElseThrow();
        User retrievedTempUser = userRepository.findByUsername("tempuser").orElseThrow();

        // Assert
        assertThat(retrievedNormalUser.isMustChangePassword()).isFalse();
        assertThat(retrievedNormalUser.isTemporaryPassword()).isFalse();

        assertThat(retrievedTempUser.isMustChangePassword()).isTrue();
        assertThat(retrievedTempUser.isTemporaryPassword()).isTrue();
    }
}
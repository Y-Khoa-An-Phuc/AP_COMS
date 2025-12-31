package com.src.ap.repository;

import com.src.ap.entity.Role;
import com.src.ap.entity.RoleName;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("RoleRepository Tests")
class RoleRepositoryTest {

    @Autowired
    private RoleRepository roleRepository;

    @Test
    @DisplayName("Should save a new role successfully")
    void testSaveRole() {
        // Arrange
        Role role = new Role(RoleName.USER);

        // Act
        Role savedRole = roleRepository.save(role);

        // Assert
        assertThat(savedRole).isNotNull();
        assertThat(savedRole.getId()).isNotNull();
        assertThat(savedRole.getName()).isEqualTo(RoleName.USER);
    }

    @Test
    @DisplayName("Should find role by name when role exists")
    void testFindByName_WhenRoleExists() {
        // Arrange
        Role role = new Role(RoleName.TECHADMIN);
        roleRepository.save(role);

        // Act
        Optional<Role> foundRole = roleRepository.findByName(RoleName.TECHADMIN);

        // Assert
        assertThat(foundRole).isPresent();
        assertThat(foundRole.get().getName()).isEqualTo(RoleName.TECHADMIN);
    }

    @Test
    @DisplayName("Should return empty Optional when role does not exist")
    void testFindByName_WhenRoleDoesNotExist() {
        // Act
        Optional<Role> foundRole = roleRepository.findByName(RoleName.USER);

        // Assert
        assertThat(foundRole).isEmpty();
    }

    @Test
    @DisplayName("Should find all role names")
    void testFindAllRoleNames() {
        // Arrange
        roleRepository.save(new Role(RoleName.USER));
        roleRepository.save(new Role(RoleName.TECHADMIN));

        // Act
        Iterable<Role> roles = roleRepository.findAll();

        // Assert
        assertThat(roles).hasSize(2);
        assertThat(roles).extracting(Role::getName)
                .containsExactlyInAnyOrder(RoleName.USER, RoleName.TECHADMIN);
    }

    @Test
    @DisplayName("Should save role with USER name")
    void testSaveUserRole() {
        // Arrange
        Role role = Role.builder()
                .name(RoleName.USER)
                .build();

        // Act
        Role savedRole = roleRepository.save(role);

        // Assert
        assertThat(savedRole).isNotNull();
        assertThat(savedRole.getId()).isNotNull();
        assertThat(savedRole.getName()).isEqualTo(RoleName.USER);
    }

    @Test
    @DisplayName("Should save role with ADMIN name")
    void testSaveAdminRole() {
        // Arrange
        Role role = Role.builder()
                .name(RoleName.TECHADMIN)
                .build();

        // Act
        Role savedRole = roleRepository.save(role);

        // Assert
        assertThat(savedRole).isNotNull();
        assertThat(savedRole.getId()).isNotNull();
        assertThat(savedRole.getName()).isEqualTo(RoleName.TECHADMIN);
    }

    @Test
    @DisplayName("Should delete role successfully")
    void testDeleteRole() {
        // Arrange
        Role role = new Role(RoleName.USER);
        Role savedRole = roleRepository.save(role);
        Long roleId = savedRole.getId();

        // Act
        roleRepository.delete(savedRole);

        // Assert
        Optional<Role> deletedRole = roleRepository.findById(roleId);
        assertThat(deletedRole).isEmpty();
    }

    @Test
    @DisplayName("Should count roles correctly")
    void testCountRoles() {
        // Arrange
        roleRepository.save(new Role(RoleName.USER));
        roleRepository.save(new Role(RoleName.TECHADMIN));

        // Act
        long count = roleRepository.count();

        // Assert
        assertThat(count).isEqualTo(2);
    }
}
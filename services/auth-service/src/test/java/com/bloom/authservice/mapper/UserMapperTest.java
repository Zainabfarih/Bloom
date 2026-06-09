package com.bloom.authservice.mapper;

import com.bloom.authservice.dto.UserDTO;
import com.bloom.authservice.entity.Role;
import com.bloom.authservice.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class UserMapperTest {

    private final UserMapper mapper = new UserMapper();

    @Test
    @DisplayName("toDTO : mappe les champs publics, n'expose pas le password ni les flags admin")
    void toDTO_maps_public_fields_only() {
        User user = User.builder()
                .id(1L).email("alice@bloom.dev").password("$2a$12$secret")
                .firstName("Alice").lastName("Martin").role(Role.STUDENT)
                .createdAt(LocalDateTime.now()).enabled(true).locked(false).deleted(false).build();

        UserDTO dto = mapper.toDTO(user);

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getEmail()).isEqualTo("alice@bloom.dev");
        assertThat(dto.getRole()).isEqualTo("STUDENT");
        assertThat(dto.getFirstName()).isEqualTo("Alice");
        // public DTO ne doit pas inclure les champs admin
        assertThat(dto.getCreatedAt()).isNull();
        assertThat(dto.getEnabled()).isNull();
        assertThat(dto.getLocked()).isNull();
        assertThat(dto.getDeleted()).isNull();
    }

    @Test
    @DisplayName("toDTO : null → null")
    void toDTO_returns_null_when_input_null() {
        assertThat(mapper.toDTO(null)).isNull();
    }

    @Test
    @DisplayName("toAdminDTO : inclut tous les champs admin (createdAt, enabled, locked, deleted)")
    void toAdminDTO_includes_audit_fields() {
        LocalDateTime created = LocalDateTime.of(2025, 1, 1, 12, 0);
        User user = User.builder()
                .id(1L).email("bob@bloom.dev").password("h")
                .firstName("Bob").lastName("Durand").role(Role.STUDENT)
                .createdAt(created).enabled(true).locked(true).deleted(false)
                .failedLoginAttempts(2).build();

        UserDTO dto = mapper.toAdminDTO(user);

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getCreatedAt()).isEqualTo(created);
        assertThat(dto.getEnabled()).isTrue();
        assertThat(dto.getLocked()).isTrue();
        assertThat(dto.getDeleted()).isFalse();
    }

    @Test
    @DisplayName("toAdminDTO : null → null")
    void toAdminDTO_returns_null_when_input_null() {
        assertThat(mapper.toAdminDTO(null)).isNull();
    }
}

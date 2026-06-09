package com.bloom.authservice.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import static org.assertj.core.api.Assertions.assertThat;

class RoleTest {

    @Test
    @DisplayName("STUDENT.getAuthorities → ROLE_STUDENT")
    void student_authorities() {
        assertThat(Role.STUDENT.getAuthorities())
                .containsExactly(new SimpleGrantedAuthority("ROLE_STUDENT"));
    }

    @Test
    @DisplayName("ADMIN.getAuthorities → ROLE_ADMIN")
    void admin_authorities() {
        assertThat(Role.ADMIN.getAuthorities())
                .containsExactly(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }
}

package com.bloom.authservice.security;

import com.bloom.authservice.entity.Role;
import com.bloom.authservice.entity.User;
import com.bloom.authservice.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService service;

    @Test
    @DisplayName("loadUserByUsername : retourne l'entité User si l'email existe")
    void load_returns_user_when_found() {
        User user = User.builder()
                .id(1L).email("alice@bloom.dev").password("h")
                .firstName("A").lastName("M").role(Role.STUDENT).build();

        when(userRepository.findByEmail("alice@bloom.dev")).thenReturn(Optional.of(user));

        var result = service.loadUserByUsername("alice@bloom.dev");
        assertThat(result).isEqualTo(user);
    }

    @Test
    @DisplayName("loadUserByUsername : lève UsernameNotFoundException si l'email est inconnu")
    void load_throws_when_not_found() {
        when(userRepository.findByEmail("ghost@bloom.dev")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("ghost@bloom.dev"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("ghost@bloom.dev");
    }
}

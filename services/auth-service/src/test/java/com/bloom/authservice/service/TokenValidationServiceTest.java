package com.bloom.authservice.service;

import com.bloom.authservice.dto.TokenValidationResponse;
import com.bloom.authservice.entity.Role;
import com.bloom.authservice.entity.User;
import com.bloom.authservice.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetailsService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenValidationServiceTest {

    @Mock private JwtService jwtService;
    @Mock private UserDetailsService userDetailsService;

    @InjectMocks
    private TokenValidationService service;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).email("alice@bloom.dev").password("h")
                .firstName("A").lastName("M").role(Role.STUDENT).build();
    }

    @Test
    @DisplayName("validateToken : token valide → réponse avec userId/email/role")
    void validate_returns_valid_response() {
        when(jwtService.extractUsername("token")).thenReturn("alice@bloom.dev");
        when(userDetailsService.loadUserByUsername("alice@bloom.dev")).thenReturn(user);
        when(jwtService.isTokenValid("token", user)).thenReturn(true);
        when(jwtService.extractUserId("token")).thenReturn("1");
        when(jwtService.extractRole("token")).thenReturn("STUDENT");

        TokenValidationResponse response = service.validateToken("token");

        assertThat(response.isValid()).isTrue();
        assertThat(response.getUserId()).isEqualTo("1");
        assertThat(response.getEmail()).isEqualTo("alice@bloom.dev");
        assertThat(response.getRole()).isEqualTo("STUDENT");
    }

    @Test
    @DisplayName("validateToken : token invalide → valid=false")
    void validate_returns_invalid_when_token_not_valid() {
        when(jwtService.extractUsername("token")).thenReturn("alice@bloom.dev");
        when(userDetailsService.loadUserByUsername("alice@bloom.dev")).thenReturn(user);
        when(jwtService.isTokenValid("token", user)).thenReturn(false);

        TokenValidationResponse response = service.validateToken("token");

        assertThat(response.isValid()).isFalse();
        assertThat(response.getUserId()).isNull();
    }

    @Test
    @DisplayName("validateToken : exception → valid=false (catch silencieux)")
    void validate_swallows_exceptions_and_returns_invalid() {
        when(jwtService.extractUsername("bad"))
                .thenThrow(new io.jsonwebtoken.MalformedJwtException("nope"));

        TokenValidationResponse response = service.validateToken("bad");

        assertThat(response.isValid()).isFalse();
    }
}

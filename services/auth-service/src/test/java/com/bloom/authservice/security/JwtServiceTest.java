package com.bloom.authservice.security;

import com.bloom.authservice.entity.Role;
import com.bloom.authservice.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String JWT_SECRET =
            "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";

    private JwtService jwtService;
    private User user;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", JWT_SECRET);
        ReflectionTestUtils.setField(jwtService, "accessTokenExpiration", 900_000L);

        user = User.builder()
                .id(42L)
                .firstName("Alice")
                .lastName("Martin")
                .email("alice@bloom.dev")
                .password("hash")
                .role(Role.STUDENT)
                .build();
    }

    @Test
    @DisplayName("generateAccessToken : produit un JWT signé contenant subject, userId et role")
    void generateAccessToken_contains_expected_claims() {
        String token = jwtService.generateAccessToken(user);

        assertThat(token).isNotBlank();
        // JWT structure : header.payload.signature
        assertThat(token.split("\\.")).hasSize(3);

        assertThat(jwtService.extractUsername(token)).isEqualTo("alice@bloom.dev");
        assertThat(jwtService.extractUserId(token)).isEqualTo("42");
        assertThat(jwtService.extractRole(token)).isEqualTo("STUDENT");
    }

    @Test
    @DisplayName("isTokenValid : retourne true pour un token valide non expiré")
    void isTokenValid_returns_true_for_valid_token() {
        String token = jwtService.generateAccessToken(user);

        assertThat(jwtService.isTokenValid(token, user)).isTrue();
    }

    @Test
    @DisplayName("isTokenValid : retourne false si l'utilisateur ne correspond pas au sujet")
    void isTokenValid_returns_false_for_different_user() {
        String token = jwtService.generateAccessToken(user);

        User other = User.builder()
                .id(99L)
                .firstName("Bob")
                .lastName("Durand")
                .email("bob@bloom.dev")
                .password("hash")
                .role(Role.STUDENT)
                .build();

        assertThat(jwtService.isTokenValid(token, other)).isFalse();
    }

    @Test
    @DisplayName("isTokenValid : retourne false pour un token expiré")
    void isTokenValid_returns_false_for_expired_token() {
        ReflectionTestUtils.setField(jwtService, "accessTokenExpiration", -1_000L); // déjà expiré
        String expired = jwtService.generateAccessToken(user);

        // L'extraction d'expiration doit lever une JwtException → on attend false ou exception
        assertThatThrownBy(() -> jwtService.isTokenValid(expired, user))
                .isInstanceOf(io.jsonwebtoken.ExpiredJwtException.class);
    }

    @Test
    @DisplayName("extractUsername : lève une exception si le token est falsifié")
    void extractUsername_throws_when_token_is_corrupted() {
        String token = jwtService.generateAccessToken(user);
        // Corrompre la signature (dernier segment)
        String corrupted = token.substring(0, token.lastIndexOf('.') + 1) + "AAAA";

        assertThatThrownBy(() -> jwtService.extractUsername(corrupted))
                .isInstanceOf(io.jsonwebtoken.JwtException.class);
    }

    @Test
    @DisplayName("extractRole : retourne la valeur du claim role")
    void extractRole_returns_role_claim() {
        User admin = User.builder()
                .id(1L)
                .email("admin@bloom.dev")
                .password("h")
                .firstName("A")
                .lastName("D")
                .role(Role.ADMIN)
                .build();

        String token = jwtService.generateAccessToken(admin);
        assertThat(jwtService.extractRole(token)).isEqualTo("ADMIN");
    }
}

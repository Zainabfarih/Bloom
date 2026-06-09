package com.bloom.authservice.service;

import com.bloom.authservice.entity.RefreshToken;
import com.bloom.authservice.entity.Role;
import com.bloom.authservice.entity.User;
import com.bloom.authservice.repository.RefreshTokenRepository;
import com.bloom.authservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    private User user;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(refreshTokenService, "refreshTokenExpiration", 604_800_000L);
        user = User.builder().id(1L).email("alice@bloom.dev").password("h")
                .firstName("A").lastName("M").role(Role.STUDENT).build();
    }

    @Test
    @DisplayName("createRefreshToken : révoque les anciens, retourne un nouveau token persisté")
    void createRefreshToken_revokes_existing_and_creates_new() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> {
            RefreshToken t = inv.getArgument(0);
            t.setId(7L);
            return t;
        });

        RefreshToken result = refreshTokenService.createRefreshToken(1L);

        verify(refreshTokenRepository).revokeAllByUserId(1L);
        assertThat(result.getId()).isEqualTo(7L);
        assertThat(result.getUser()).isEqualTo(user);
        assertThat(result.getToken()).isNotBlank();
        assertThat(result.getExpiryDate()).isAfter(Instant.now());
    }

    @Test
    @DisplayName("createRefreshToken : user inconnu → RuntimeException")
    void createRefreshToken_throws_when_user_unknown() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.createRefreshToken(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    @DisplayName("verifyExpiration : token valide → retourne le token tel quel")
    void verifyExpiration_returns_token_when_valid() {
        RefreshToken token = RefreshToken.builder()
                .id(1L).token("t").user(user).revoked(false)
                .expiryDate(Instant.now().plusSeconds(60)).build();

        assertThat(refreshTokenService.verifyExpiration(token)).isEqualTo(token);
    }

    @Test
    @DisplayName("verifyExpiration : token révoqué → RuntimeException")
    void verifyExpiration_throws_when_revoked() {
        RefreshToken token = RefreshToken.builder()
                .id(1L).token("t").user(user).revoked(true)
                .expiryDate(Instant.now().plusSeconds(60)).build();

        assertThatThrownBy(() -> refreshTokenService.verifyExpiration(token))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("revoked");
    }

    @Test
    @DisplayName("verifyExpiration : token expiré → supprime + RuntimeException")
    void verifyExpiration_throws_and_deletes_when_expired() {
        RefreshToken token = RefreshToken.builder()
                .id(1L).token("t").user(user).revoked(false)
                .expiryDate(Instant.now().minusSeconds(1)).build();

        assertThatThrownBy(() -> refreshTokenService.verifyExpiration(token))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("expired");

        verify(refreshTokenRepository).delete(token);
    }

    @Test
    @DisplayName("findByToken : délègue à la repo")
    void findByToken_delegates_to_repo() {
        RefreshToken t = RefreshToken.builder().id(1L).token("xyz").user(user)
                .expiryDate(Instant.now().plusSeconds(10)).build();
        when(refreshTokenRepository.findByToken("xyz")).thenReturn(Optional.of(t));

        assertThat(refreshTokenService.findByToken("xyz")).contains(t);
    }

    @Test
    @DisplayName("revokeByUserId : appelle revokeAllByUserId sur la repo")
    void revokeByUserId_calls_repo() {
        refreshTokenService.revokeByUserId(1L);
        verify(refreshTokenRepository).revokeAllByUserId(1L);
    }

    @Test
    @DisplayName("revokeToken : token connu → marque comme révoqué et sauvegarde")
    void revokeToken_marks_revoked_when_found() {
        RefreshToken t = RefreshToken.builder().id(1L).token("xyz").user(user)
                .revoked(false).expiryDate(Instant.now().plusSeconds(10)).build();
        when(refreshTokenRepository.findByToken("xyz")).thenReturn(Optional.of(t));

        refreshTokenService.revokeToken("xyz");

        assertThat(t.isRevoked()).isTrue();
        verify(refreshTokenRepository).save(t);
    }

    @Test
    @DisplayName("revokeToken : token inconnu → no-op silencieux")
    void revokeToken_silent_when_unknown() {
        when(refreshTokenRepository.findByToken("ghost")).thenReturn(Optional.empty());
        refreshTokenService.revokeToken("ghost");
        verify(refreshTokenRepository, never()).save(any());
    }
}

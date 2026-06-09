package com.bloom.authservice.service;

import com.bloom.authservice.dto.PasswordResetRequest;
import com.bloom.authservice.dto.PasswordUpdateRequest;
import com.bloom.authservice.entity.PasswordResetToken;
import com.bloom.authservice.entity.Role;
import com.bloom.authservice.entity.User;
import com.bloom.authservice.exception.InvalidTokenException;
import com.bloom.authservice.repository.PasswordResetTokenRepository;
import com.bloom.authservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordResetTokenRepository tokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private EmailService emailService;

    @InjectMocks
    private PasswordService passwordService;

    private User user;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(passwordService, "tokenExpiration", 3_600_000L);

        user = User.builder()
                .id(1L)
                .email("alice@bloom.dev")
                .password("$2a$12$old")
                .firstName("Alice")
                .lastName("Martin")
                .role(Role.STUDENT)
                .emailVerified(true)
                .build();
    }

    // ─── initiatePasswordReset ─────────────────────────────────────────

    @Test
    @DisplayName("initiatePasswordReset : crée un token et délègue l'envoi à EmailService")
    void initiate_creates_token_and_delegates_email() {
        PasswordResetRequest req = new PasswordResetRequest();
        req.setEmail("alice@bloom.dev");

        when(userRepository.findByEmail("alice@bloom.dev")).thenReturn(Optional.of(user));
        when(tokenRepository.save(any(PasswordResetToken.class))).thenAnswer(inv -> {
            PasswordResetToken t = inv.getArgument(0);
            t.setId(42L);
            return t;
        });

        passwordService.initiatePasswordReset(req);

        verify(tokenRepository).deleteByUserId(1L);
        verify(tokenRepository).save(any(PasswordResetToken.class));
        verify(emailService).sendPasswordResetEmail(eq("alice@bloom.dev"), anyString());
    }

    @Test
    @DisplayName("initiatePasswordReset : email inconnu → no-op silencieux (anti user-enum)")
    void initiate_silent_when_email_unknown() {
        PasswordResetRequest req = new PasswordResetRequest();
        req.setEmail("ghost@bloom.dev");
        when(userRepository.findByEmail("ghost@bloom.dev")).thenReturn(Optional.empty());

        passwordService.initiatePasswordReset(req);

        verify(tokenRepository, never()).save(any());
        verifyNoInteractions(emailService);
    }

    // ─── updatePassword ────────────────────────────────────────────────

    @Test
    @DisplayName("updatePassword : succès — encode le nouveau password, marque le token comme utilisé")
    void update_password_success() {
        PasswordResetToken token = PasswordResetToken.builder()
                .id(1L).token("valid-token").used(false)
                .expiryDate(Instant.now().plusSeconds(3600)).user(user).build();

        when(tokenRepository.findByToken("valid-token")).thenReturn(Optional.of(token));
        when(passwordEncoder.encode("newPass123")).thenReturn("$2a$12$new");

        PasswordUpdateRequest req = new PasswordUpdateRequest();
        req.setToken("valid-token");
        req.setNewPassword("newPass123");

        passwordService.updatePassword(req);

        assertThat(user.getPassword()).isEqualTo("$2a$12$new");
        assertThat(token.isUsed()).isTrue();
        verify(userRepository).save(user);
        verify(tokenRepository).save(token);
    }

    @Test
    @DisplayName("updatePassword : token déjà utilisé → InvalidTokenException")
    void update_password_throws_when_token_used() {
        PasswordResetToken token = PasswordResetToken.builder()
                .id(1L).token("used-token").used(true)
                .expiryDate(Instant.now().plusSeconds(3600)).user(user).build();

        when(tokenRepository.findByToken("used-token")).thenReturn(Optional.of(token));

        PasswordUpdateRequest req = new PasswordUpdateRequest();
        req.setToken("used-token");
        req.setNewPassword("anything");

        assertThatThrownBy(() -> passwordService.updatePassword(req))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    @DisplayName("updatePassword : token expiré → InvalidTokenException")
    void update_password_throws_when_token_expired() {
        PasswordResetToken token = PasswordResetToken.builder()
                .id(1L).token("expired-token").used(false)
                .expiryDate(Instant.now().minusSeconds(60)).user(user).build();

        when(tokenRepository.findByToken("expired-token")).thenReturn(Optional.of(token));

        PasswordUpdateRequest req = new PasswordUpdateRequest();
        req.setToken("expired-token");
        req.setNewPassword("anything");

        assertThatThrownBy(() -> passwordService.updatePassword(req))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    @DisplayName("updatePassword : token inconnu → InvalidTokenException")
    void update_password_throws_when_token_unknown() {
        when(tokenRepository.findByToken("ghost")).thenReturn(Optional.empty());

        PasswordUpdateRequest req = new PasswordUpdateRequest();
        req.setToken("ghost");
        req.setNewPassword("any");

        assertThatThrownBy(() -> passwordService.updatePassword(req))
                .isInstanceOf(InvalidTokenException.class);
    }
}

package com.bloom.authservice.service;

import com.bloom.authservice.entity.EmailVerificationToken;
import com.bloom.authservice.entity.Role;
import com.bloom.authservice.entity.User;
import com.bloom.authservice.exception.InvalidTokenException;
import com.bloom.authservice.repository.EmailVerificationTokenRepository;
import com.bloom.authservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private EmailVerificationTokenRepository tokenRepository;
    @Mock private EmailService emailService;

    @InjectMocks
    private EmailVerificationService service;

    private User user;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "tokenExpiration", 86_400_000L); // 24h

        user = User.builder()
                .id(1L)
                .email("alice@bloom.dev")
                .password("$2a$12$h")
                .firstName("Alice")
                .lastName("Martin")
                .role(Role.STUDENT)
                .emailVerified(false)
                .build();
    }

    // ─── initiateVerification ─────────────────────────────────────────

    @Test
    @DisplayName("initiateVerification : supprime les anciens tokens, persiste un nouveau, envoie le mail")
    void initiateVerification_creates_token_and_sends_email() {
        when(tokenRepository.save(any(EmailVerificationToken.class))).thenAnswer(inv -> {
            EmailVerificationToken t = inv.getArgument(0);
            t.setId(7L);
            return t;
        });

        service.initiateVerification(user);

        verify(tokenRepository).deleteByUserId(1L);

        ArgumentCaptor<EmailVerificationToken> captor =
                ArgumentCaptor.forClass(EmailVerificationToken.class);
        verify(tokenRepository).save(captor.capture());
        EmailVerificationToken saved = captor.getValue();
        assertThat(saved.getToken()).isNotBlank();
        assertThat(saved.getExpiryDate()).isAfter(Instant.now());
        assertThat(saved.getUser()).isEqualTo(user);
        assertThat(saved.isUsed()).isFalse();

        verify(emailService).sendVerificationEmail(eq("alice@bloom.dev"), anyString());
    }

    @Test
    @DisplayName("initiateVerification : user déjà vérifié → no-op silencieux")
    void initiateVerification_skips_when_user_already_verified() {
        user.setEmailVerified(true);

        service.initiateVerification(user);

        verifyNoInteractions(tokenRepository);
        verifyNoInteractions(emailService);
    }

    // ─── verifyEmail ──────────────────────────────────────────────────

    @Test
    @DisplayName("verifyEmail : token valide → marque user vérifié et token utilisé")
    void verifyEmail_marks_user_verified_and_token_used() {
        EmailVerificationToken token = EmailVerificationToken.builder()
                .id(1L).token("valid").used(false)
                .expiryDate(Instant.now().plusSeconds(60)).user(user).build();

        when(tokenRepository.findByToken("valid")).thenReturn(Optional.of(token));

        service.verifyEmail("valid");

        assertThat(user.isEmailVerified()).isTrue();
        assertThat(token.isUsed()).isTrue();
        verify(userRepository).save(user);
        verify(tokenRepository).save(token);
    }

    @Test
    @DisplayName("verifyEmail : token inconnu → InvalidTokenException")
    void verifyEmail_throws_when_token_unknown() {
        when(tokenRepository.findByToken("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.verifyEmail("ghost"))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    @DisplayName("verifyEmail : token déjà utilisé → InvalidTokenException")
    void verifyEmail_throws_when_token_used() {
        EmailVerificationToken token = EmailVerificationToken.builder()
                .id(1L).token("used").used(true)
                .expiryDate(Instant.now().plusSeconds(60)).user(user).build();

        when(tokenRepository.findByToken("used")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.verifyEmail("used"))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    @DisplayName("verifyEmail : token expiré → InvalidTokenException")
    void verifyEmail_throws_when_token_expired() {
        EmailVerificationToken token = EmailVerificationToken.builder()
                .id(1L).token("expired").used(false)
                .expiryDate(Instant.now().minusSeconds(60)).user(user).build();

        when(tokenRepository.findByToken("expired")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.verifyEmail("expired"))
                .isInstanceOf(InvalidTokenException.class);
    }

    // ─── resendVerification ───────────────────────────────────────────

    @Test
    @DisplayName("resendVerification : user non vérifié → renvoie un mail")
    void resendVerification_resends_when_unverified() {
        when(userRepository.findByEmail("alice@bloom.dev")).thenReturn(Optional.of(user));
        when(tokenRepository.save(any(EmailVerificationToken.class))).thenAnswer(inv -> inv.getArgument(0));

        service.resendVerification("alice@bloom.dev");

        verify(emailService).sendVerificationEmail(eq("alice@bloom.dev"), anyString());
    }

    @Test
    @DisplayName("resendVerification : user déjà vérifié → no-op (pas de mail)")
    void resendVerification_silent_when_already_verified() {
        user.setEmailVerified(true);
        when(userRepository.findByEmail("alice@bloom.dev")).thenReturn(Optional.of(user));

        service.resendVerification("alice@bloom.dev");

        verifyNoInteractions(emailService);
    }

    @Test
    @DisplayName("resendVerification : email inconnu → no-op silencieux (anti user-enum)")
    void resendVerification_silent_when_email_unknown() {
        when(userRepository.findByEmail("ghost@bloom.dev")).thenReturn(Optional.empty());

        service.resendVerification("ghost@bloom.dev");

        verifyNoInteractions(emailService);
        verify(tokenRepository, never()).save(any());
    }
}

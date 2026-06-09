package com.bloom.authservice.service;

import com.bloom.authservice.dto.*;
import com.bloom.authservice.entity.RefreshToken;
import com.bloom.authservice.entity.Role;
import com.bloom.authservice.entity.User;
import com.bloom.authservice.repository.UserRepository;
import com.bloom.authservice.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    private User user;
    private RefreshToken refreshToken;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "maxFailedAttempts", 5);

        user = User.builder()
                .id(1L)
                .firstName("Alice")
                .lastName("Martin")
                .email("alice@bloom.dev")
                .password("$2a$12$encoded")
                .role(Role.STUDENT)
                .build();

        refreshToken = RefreshToken.builder()
                .id(1L)
                .token("refresh-uuid")
                .expiryDate(Instant.now().plusSeconds(3600))
                .user(user)
                .build();
    }

    // ─── register ──────────────────────────────────────────────────────

    @Test
    @DisplayName("register : succès — crée l'utilisateur, hash le password, retourne tokens")
    void register_creates_user_and_returns_tokens() {
        RegisterRequest req = new RegisterRequest();
        req.setFirstName("Alice");
        req.setLastName("Martin");
        req.setEmail("alice@bloom.dev");
        req.setPassword("password123");

        when(userRepository.existsByEmail("alice@bloom.dev")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("$2a$12$encoded");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(jwtService.generateAccessToken(user)).thenReturn("jwt-token");
        when(refreshTokenService.createRefreshToken(1L)).thenReturn(refreshToken);

        AuthResponse response = authService.register(req);

        assertThat(response.getAccessToken()).isEqualTo("jwt-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-uuid");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getUser().getEmail()).isEqualTo("alice@bloom.dev");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("register : email déjà utilisé → IllegalArgumentException")
    void register_throws_when_email_already_in_use() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("alice@bloom.dev");
        req.setPassword("password123");
        req.setFirstName("Alice");
        req.setLastName("Martin");

        when(userRepository.existsByEmail("alice@bloom.dev")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email already in use");

        verify(userRepository, never()).save(any());
    }

    // ─── login ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("login : succès — retourne tokens et reset failed attempts")
    void login_success_returns_tokens_and_resets_attempts() {
        user.setFailedLoginAttempts(2);

        LoginRequest req = new LoginRequest();
        req.setEmail("alice@bloom.dev");
        req.setPassword("password123");

        when(userRepository.findByEmail("alice@bloom.dev")).thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null);
        when(jwtService.generateAccessToken(user)).thenReturn("jwt");
        when(refreshTokenService.createRefreshToken(1L)).thenReturn(refreshToken);

        AuthResponse response = authService.login(req);

        assertThat(response.getAccessToken()).isEqualTo("jwt");
        assertThat(user.getFailedLoginAttempts()).isZero();
        verify(userRepository).save(user); // reset persistence
    }

    @Test
    @DisplayName("login : email inconnu → BadCredentialsException")
    void login_throws_when_user_not_found() {
        LoginRequest req = new LoginRequest();
        req.setEmail("ghost@bloom.dev");
        req.setPassword("any");

        when(userRepository.findByEmail("ghost@bloom.dev")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("login : compte verrouillé → LockedException")
    void login_throws_when_account_locked() {
        user.setLocked(true);

        LoginRequest req = new LoginRequest();
        req.setEmail("alice@bloom.dev");
        req.setPassword("anything");

        when(userRepository.findByEmail("alice@bloom.dev")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(LockedException.class);

        verifyNoInteractions(authenticationManager);
    }

    @Test
    @DisplayName("login : mauvais mot de passe → BadCredentialsException + incrément failed attempts")
    void login_throws_and_records_failed_attempt_on_bad_password() {
        LoginRequest req = new LoginRequest();
        req.setEmail("alice@bloom.dev");
        req.setPassword("wrong");

        when(userRepository.findByEmail("alice@bloom.dev")).thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("bad"));

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Invalid email or password");

        verify(userRepository).save(any(User.class)); // recordFailedLogin sauve l'incrément
    }

    @Test
    @DisplayName("recordFailedLogin : verrouille le compte au seuil maxFailedAttempts")
    void recordFailedLogin_locks_account_at_threshold() {
        user.setFailedLoginAttempts(4); // un de moins que 5
        when(userRepository.findByEmail("alice@bloom.dev")).thenReturn(Optional.of(user));

        authService.recordFailedLogin("alice@bloom.dev");

        assertThat(user.getFailedLoginAttempts()).isEqualTo(5);
        assertThat(user.isLocked()).isTrue();
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("recordFailedLogin : email inconnu → no-op silencieux")
    void recordFailedLogin_silent_when_user_unknown() {
        when(userRepository.findByEmail("nope@bloom.dev")).thenReturn(Optional.empty());

        authService.recordFailedLogin("nope@bloom.dev");

        verify(userRepository, never()).save(any());
    }

    // ─── refreshToken ──────────────────────────────────────────────────

    @Test
    @DisplayName("refreshToken : succès — retourne un nouveau access token")
    void refresh_token_returns_new_access_token() {
        RefreshTokenRequest req = new RefreshTokenRequest();
        req.setRefreshToken("refresh-uuid");

        when(refreshTokenService.findByToken("refresh-uuid")).thenReturn(Optional.of(refreshToken));
        when(refreshTokenService.verifyExpiration(refreshToken)).thenReturn(refreshToken);
        when(jwtService.generateAccessToken(user)).thenReturn("new-jwt");
        when(refreshTokenService.createRefreshToken(1L)).thenReturn(refreshToken);

        AuthResponse response = authService.refreshToken(req);

        assertThat(response.getAccessToken()).isEqualTo("new-jwt");
        assertThat(response.getUser().getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("refreshToken : token inconnu → RuntimeException")
    void refresh_token_throws_when_token_unknown() {
        RefreshTokenRequest req = new RefreshTokenRequest();
        req.setRefreshToken("ghost-token");

        when(refreshTokenService.findByToken("ghost-token")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refreshToken(req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Refresh token not found");
    }

    // ─── logout ────────────────────────────────────────────────────────

    @Test
    @DisplayName("logout : délègue la révocation au RefreshTokenService")
    void logout_delegates_to_refresh_token_service() {
        authService.logout("refresh-uuid");
        verify(refreshTokenService).revokeToken("refresh-uuid");
    }
}

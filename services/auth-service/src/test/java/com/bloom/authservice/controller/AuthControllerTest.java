package com.bloom.authservice.controller;

import com.bloom.authservice.dto.*;
import com.bloom.authservice.exception.GlobalExceptionHandler;
import com.bloom.authservice.security.JwtAuthFilter;
import com.bloom.authservice.security.JwtService;
import com.bloom.authservice.service.AuthService;
import com.bloom.authservice.service.PasswordService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private AuthService authService;
    @MockitoBean private PasswordService passwordService;

    // Beans transitivement requis par JwtAuthFilter (@Component scanné par @WebMvcTest)
    @MockitoBean private JwtAuthFilter jwtAuthFilter;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private UserDetailsService userDetailsService;

    private AuthResponse okResponse() {
        return AuthResponse.builder()
                .accessToken("jwt")
                .refreshToken("refresh-uuid")
                .tokenType("Bearer")
                .user(UserDTO.builder().id(1L).email("alice@bloom.dev").role("STUDENT").build())
                .build();
    }

    @Nested
    @DisplayName("POST /api/auth/register")
    class Register {

        @Test
        @DisplayName("200 — succès")
        void register_returns_200() throws Exception {
            when(authService.register(any())).thenReturn(okResponse());

            RegisterRequest req = new RegisterRequest();
            req.setFirstName("Alice");
            req.setLastName("Martin");
            req.setEmail("alice@bloom.dev");
            req.setPassword("password123");

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").value("jwt"))
                    .andExpect(jsonPath("$.user.email").value("alice@bloom.dev"));
        }

        @Test
        @DisplayName("400 — email manquant (Bean Validation)")
        void register_returns_400_when_invalid() throws Exception {
            RegisterRequest req = new RegisterRequest();
            req.setFirstName("Alice");
            req.setLastName("Martin");
            req.setEmail("");          // @NotBlank
            req.setPassword("short");  // @Size(min=8)

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Validation Failed"));
        }
    }

    @Nested
    @DisplayName("POST /api/auth/login")
    class Login {

        @Test
        @DisplayName("200 — succès")
        void login_returns_200() throws Exception {
            when(authService.login(any())).thenReturn(okResponse());

            LoginRequest req = new LoginRequest();
            req.setEmail("alice@bloom.dev");
            req.setPassword("password123");

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").value("jwt"));
        }

        @Test
        @DisplayName("401 — mauvais credentials → message générique")
        void login_returns_401_on_bad_credentials() throws Exception {
            when(authService.login(any())).thenThrow(new BadCredentialsException("internal"));

            LoginRequest req = new LoginRequest();
            req.setEmail("alice@bloom.dev");
            req.setPassword("password123");

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Invalid email or password"));
        }
    }

    @Nested
    @DisplayName("POST /api/auth/refresh & /api/auth/logout")
    class TokenLifecycle {

        @Test
        @DisplayName("refresh — 200")
        void refresh_returns_200() throws Exception {
            when(authService.refreshToken(any())).thenReturn(okResponse());

            RefreshTokenRequest req = new RefreshTokenRequest();
            req.setRefreshToken("refresh-uuid");

            mockMvc.perform(post("/api/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").value("jwt"));
        }

        @Test
        @DisplayName("logout — 204 + délègue au service")
        void logout_returns_204() throws Exception {
            RefreshTokenRequest req = new RefreshTokenRequest();
            req.setRefreshToken("refresh-uuid");

            mockMvc.perform(post("/api/auth/logout")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isNoContent());

            verify(authService).logout("refresh-uuid");
        }
    }

    @Nested
    @DisplayName("POST /api/auth/password-reset/*")
    class PasswordReset {

        @Test
        @DisplayName("initiate — 200")
        void initiate_returns_200() throws Exception {
            PasswordResetRequest req = new PasswordResetRequest();
            req.setEmail("alice@bloom.dev");

            mockMvc.perform(post("/api/auth/password-reset/initiate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk());

            verify(passwordService).initiatePasswordReset(any());
        }

        @Test
        @DisplayName("update — 200")
        void update_returns_200() throws Exception {
            PasswordUpdateRequest req = new PasswordUpdateRequest();
            req.setToken("valid-token");
            req.setNewPassword("newPass1234");

            mockMvc.perform(post("/api/auth/password-reset/update")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk());

            verify(passwordService).updatePassword(any());
        }

        @Test
        @DisplayName("update — 400 quand le password est trop court")
        void update_returns_400_when_password_too_short() throws Exception {
            PasswordUpdateRequest req = new PasswordUpdateRequest();
            req.setToken("valid-token");
            req.setNewPassword("short");

            mockMvc.perform(post("/api/auth/password-reset/update")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }
    }
}

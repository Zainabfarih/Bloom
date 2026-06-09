package com.bloom.authservice.controller;

import com.bloom.authservice.dto.TokenValidationResponse;
import com.bloom.authservice.exception.GlobalExceptionHandler;
import com.bloom.authservice.security.JwtAuthFilter;
import com.bloom.authservice.security.JwtService;
import com.bloom.authservice.service.TokenValidationService;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = TokenController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@ActiveProfiles("test")
class TokenControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private TokenValidationService tokenValidationService;

    // Beans transitivement requis par JwtAuthFilter (@Component scanné par @WebMvcTest)
    @MockitoBean private JwtAuthFilter jwtAuthFilter;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private UserDetailsService userDetailsService;

    @Test
    @DisplayName("GET /api/tokens/validate — 200 avec token valide")
    void validate_returns_200_with_valid_payload() throws Exception {
        when(tokenValidationService.validateToken("good-token"))
                .thenReturn(TokenValidationResponse.builder()
                        .valid(true).userId("1").email("alice@bloom.dev").role("STUDENT").build());

        mockMvc.perform(get("/api/tokens/validate").header("Authorization", "Bearer good-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.userId").value("1"))
                .andExpect(jsonPath("$.role").value("STUDENT"));
    }

    @Test
    @DisplayName("GET /api/tokens/validate — header sans préfixe Bearer → valid=false")
    void validate_returns_invalid_when_header_not_bearer() throws Exception {
        mockMvc.perform(get("/api/tokens/validate").header("Authorization", "Basic abc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false));
    }
}

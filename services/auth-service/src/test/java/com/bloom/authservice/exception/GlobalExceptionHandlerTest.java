package com.bloom.authservice.exception;

import com.bloom.authservice.dto.ErrorResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.MapBindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.WebRequest;

import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    private WebRequest request(String uri) {
        WebRequest req = mock(WebRequest.class);
        when(req.getDescription(false)).thenReturn("uri=" + uri);
        return req;
    }

    @Test
    @DisplayName("Validation → 400 + détails par champ, sans détail technique")
    void handles_validation_error() throws NoSuchMethodException {
        BindingResult bindingResult = new MapBindingResult(new HashMap<>(), "request");
        bindingResult.rejectValue("email", "NotBlank", "email is required");

        java.lang.reflect.Method method = String.class.getMethod("toString");
        org.springframework.core.MethodParameter parameter =
                new org.springframework.core.MethodParameter(method, -1);
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(parameter, bindingResult);

        ResponseEntity<ErrorResponse> response =
                handler.handleValidationExceptions(ex, request("/api/auth/register"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getError()).isEqualTo("Validation Failed");
        assertThat(response.getBody().getValidationErrors())
                .containsEntry("email", "email is required");
        assertThat(response.getBody().getMessage()).doesNotContain("Exception");
    }

    @Test
    @DisplayName("IllegalArgumentException → 400 (email déjà utilisé, etc.)")
    void handles_illegal_argument() {
        ResponseEntity<ErrorResponse> response = handler.handleIllegalArgument(
                new IllegalArgumentException("Email déjà utilisé."), request("/api/auth/register"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getError()).isEqualTo("Bad Request");
        assertThat(response.getBody().getMessage()).contains("Email déjà utilisé");
    }

    @Test
    @DisplayName("InvalidTokenException → 400")
    void handles_invalid_token() {
        ResponseEntity<ErrorResponse> response = handler.handleInvalidToken(
                new InvalidTokenException("Lien invalide ou expiré"), request("/api/auth/verify-email"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getError()).isEqualTo("Invalid Token");
    }

    @Test
    @DisplayName("BadCredentials → 401 avec message générique")
    void handles_bad_credentials() {
        ResponseEntity<ErrorResponse> response = handler.handleBadCredentialsException(
                new BadCredentialsException("internal-detail"), request("/api/auth/login"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().getMessage()).isEqualTo("Invalid email or password");
        assertThat(response.getBody().getMessage()).doesNotContain("internal-detail");
    }

    @Test
    @DisplayName("DisabledException → 403 (email non vérifié)")
    void handles_disabled() {
        ResponseEntity<ErrorResponse> response = handler.handleDisabledException(
                new DisabledException("Email non vérifié."), request("/api/auth/login"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().getError()).isEqualTo("Email Not Verified");
    }

    @Test
    @DisplayName("LockedException → 403 (compte verrouillé)")
    void handles_locked() {
        ResponseEntity<ErrorResponse> response = handler.handleLockedException(
                new LockedException("Account locked"), request("/api/auth/login"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().getError()).isEqualTo("Account Locked");
    }

    @Test
    @DisplayName("AccessDenied → 403")
    void handles_access_denied() {
        ResponseEntity<ErrorResponse> response = handler.handleAccessDeniedException(
                new AccessDeniedException("nope"), request("/api/users/2"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().getError()).isEqualTo("Forbidden");
    }

    @Test
    @DisplayName("Exception générique → 500 sans exposer la pile technique")
    void handles_generic_exception_safely() {
        ResponseEntity<ErrorResponse> response = handler.handleGlobalException(
                new RuntimeException("DB connection refused at line 42"), request("/api/auth/login"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getMessage()).isEqualTo("An unexpected error occurred");
        assertThat(response.getBody().getMessage()).doesNotContain("DB connection refused");
    }
}

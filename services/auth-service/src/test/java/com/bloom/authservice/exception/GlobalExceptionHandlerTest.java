package com.bloom.authservice.exception;

import com.bloom.authservice.dto.ErrorResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindException;
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

        // Construire un MethodArgumentNotValidException — utilise un MethodParameter d'une méthode existante
        java.lang.reflect.Method method = String.class.getMethod("toString");
        org.springframework.core.MethodParameter parameter =
                new org.springframework.core.MethodParameter(method, -1);
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(parameter, bindingResult);

        ResponseEntity<ErrorResponse> response = handler.handleValidationExceptions(ex, request("/api/auth/register"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getError()).isEqualTo("Validation Failed");
        assertThat(response.getBody().getValidationErrors())
                .containsEntry("email", "email is required");
        assertThat(response.getBody().getMessage()).doesNotContain("Exception");
    }

    @Test
    @DisplayName("BadCredentials → 401 avec message générique")
    void handles_bad_credentials() {
        ResponseEntity<ErrorResponse> response = handler.handleBadCredentialsException(
                new BadCredentialsException("internal-detail"), request("/api/auth/login"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        // Le message générique remplace le message technique → pas de fuite d'info
        assertThat(response.getBody().getMessage()).isEqualTo("Invalid email or password");
        assertThat(response.getBody().getMessage()).doesNotContain("internal-detail");
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

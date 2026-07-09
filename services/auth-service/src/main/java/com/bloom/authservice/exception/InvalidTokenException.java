package com.bloom.authservice.exception;

/**
 * Levée quand un token (vérification d'email, refresh, reset password...)
 * est inconnu, expiré ou déjà utilisé. Mappée en 400 par le GlobalExceptionHandler.
 */
public class InvalidTokenException extends RuntimeException {
    public InvalidTokenException(String message) {
        super(message);
    }
}

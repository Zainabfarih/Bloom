package com.bloom.cvservice.exception;

/**
 * Levée lorsqu'un fichier CV ne peut être lu / parsé (PDF corrompu, vide, illisible).
 */
public class CvProcessingException extends RuntimeException {
    public CvProcessingException(String message) {
        super(message);
    }

    public CvProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}

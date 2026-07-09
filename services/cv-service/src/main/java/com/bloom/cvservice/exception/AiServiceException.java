package com.bloom.cvservice.exception;

/**
 * Levée lorsque le fournisseur d'IA (Gemini / Hugging Face) est indisponible
 * ou renvoie une réponse inexploitable lors de l'analyse ATS du CV.
 */
public class AiServiceException extends RuntimeException {
    public AiServiceException(String message) {
        super(message);
    }

    public AiServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}

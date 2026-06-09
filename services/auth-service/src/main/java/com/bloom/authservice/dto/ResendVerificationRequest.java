package com.bloom.authservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Body de POST /api/auth/verify-email/resend.
 * Renvoie un nouveau mail de vérification au compte associé.
 */
@Data
public class ResendVerificationRequest {
    @Email
    @NotBlank
    private String email;
}

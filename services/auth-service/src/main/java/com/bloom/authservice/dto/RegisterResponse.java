package com.bloom.authservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Réponse de POST /api/auth/register.
 *
 * <p>Pas de tokens : le user doit valider son email avant de pouvoir se connecter.
 * Le frontend affiche le message et redirige vers une page d'attente.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterResponse {
    private String email;
    private String message;
}

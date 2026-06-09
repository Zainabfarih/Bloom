package com.bloom.authservice.service;

import com.bloom.authservice.entity.EmailVerificationToken;
import com.bloom.authservice.entity.User;
import com.bloom.authservice.exception.InvalidTokenException;
import com.bloom.authservice.repository.EmailVerificationTokenRepository;
import com.bloom.authservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Cycle de vie d'un token de vérification d'email :
 *   • {@link #initiateVerification(User)} — appelé à l'inscription, ou à la
 *     demande de l'utilisateur via le endpoint resend.
 *   • {@link #verifyEmail(String)}        — consomme le token et marque
 *     {@code emailVerified=true} sur le user lié.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationService {

    private final UserRepository userRepository;
    private final EmailVerificationTokenRepository tokenRepository;
    private final EmailService emailService;

    /** Durée de validité du token en millisecondes. Default 24h. */
    @Value("${app.email-verification-token-expiration:86400000}")
    private long tokenExpiration;

    /**
     * Génère un nouveau token (en supprimant les éventuels précédents) et envoie
     * le mail. Idempotent : appel sur un user déjà vérifié = no-op silencieux.
     */
    @Transactional
    public void initiateVerification(User user) {
        if (user.isEmailVerified()) {
            log.debug("Skip envoi de mail de vérification — user {} déjà vérifié", user.getEmail());
            return;
        }
        tokenRepository.deleteByUserId(user.getId());
        EmailVerificationToken token = tokenRepository.save(EmailVerificationToken.builder()
                .token(UUID.randomUUID().toString())
                .expiryDate(Instant.now().plusMillis(tokenExpiration))
                .user(user)
                .build());
        emailService.sendVerificationEmail(user.getEmail(), token.getToken());
    }

    /**
     * Valide un token et marque le user comme vérifié.
     * @throws InvalidTokenException si le token est inconnu, expiré ou déjà utilisé.
     */
    @Transactional
    public void verifyEmail(String tokenValue) {
        EmailVerificationToken token = tokenRepository.findByToken(tokenValue)
                .filter(t -> !t.isUsed() && t.getExpiryDate().isAfter(Instant.now()))
                .orElseThrow(() -> new InvalidTokenException("Lien de vérification invalide ou expiré."));

        User user = token.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);

        token.setUsed(true);
        tokenRepository.save(token);
        log.info("Email vérifié pour user {}", user.getEmail());
    }

    /**
     * Renvoie un mail de vérification à l'utilisateur si son email n'est pas
     * encore validé. Pas d'erreur en cas d'email inconnu (anti user-enumeration).
     */
    @Transactional
    public void resendVerification(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            if (!user.isEmailVerified()) {
                initiateVerification(user);
            }
        });
    }
}

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
     *
     * <p><strong>Idempotent.</strong> Le lien de vérification est déclenché
     * automatiquement au chargement de la page front, et peut être appelé
     * plusieurs fois pour un même token : préchargement par les scanners
     * anti-spam / "safe links" (Gmail, Outlook…), rechargement de page, retour
     * navigateur, double-rendu React. Si le token a déjà été consommé mais que
     * le compte est bien vérifié, on considère l'opération réussie au lieu de
     * renvoyer une erreur — sinon la vérification échouerait à chaque réouverture
     * du lien.</p>
     *
     * @throws InvalidTokenException si le token est inconnu, expiré, ou déjà
     *         utilisé alors que le compte n'est pas vérifié.
     */
    @Transactional
    public void verifyEmail(String tokenValue) {
        EmailVerificationToken token = tokenRepository.findByToken(tokenValue)
                .orElseThrow(() -> new InvalidTokenException("Lien de vérification invalide ou expiré."));

        // Token déjà consommé : succès idempotent si le compte est vérifié,
        // erreur sinon (token réellement obsolète).
        if (token.isUsed()) {
            if (token.getUser().isEmailVerified()) {
                log.debug("verifyEmail : token déjà consommé, compte {} déjà vérifié — no-op",
                        token.getUser().getEmail());
                return;
            }
            throw new InvalidTokenException("Lien de vérification invalide ou expiré.");
        }

        if (token.getExpiryDate().isBefore(Instant.now())) {
            throw new InvalidTokenException("Lien de vérification invalide ou expiré.");
        }

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

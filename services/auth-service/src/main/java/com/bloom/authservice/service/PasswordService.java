package com.bloom.authservice.service;

import com.bloom.authservice.entity.PasswordResetToken;
import com.bloom.authservice.dto.PasswordResetRequest;
import com.bloom.authservice.dto.PasswordUpdateRequest;
import com.bloom.authservice.exception.InvalidTokenException;
import com.bloom.authservice.repository.PasswordResetTokenRepository;
import com.bloom.authservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Cycle de réinitialisation de mot de passe.
 * Délègue l'envoi du mail à {@link EmailService} pour ne pas dupliquer la
 * configuration SMTP côté service métier.
 */
@Service
@RequiredArgsConstructor
public class PasswordService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Value("${app.password-reset-token-expiration}")
    private long tokenExpiration;

    @Transactional
    public void initiatePasswordReset(PasswordResetRequest req) {
        userRepository.findByEmail(req.getEmail()).ifPresent(user -> {
            tokenRepository.deleteByUserId(user.getId());
            PasswordResetToken token = tokenRepository.save(PasswordResetToken.builder()
                    .token(UUID.randomUUID().toString())
                    .expiryDate(Instant.now().plusMillis(tokenExpiration))
                    .user(user)
                    .build());
            emailService.sendPasswordResetEmail(user.getEmail(), token.getToken());
        });
        // Toujours retourner 200 (anti user-enumeration)
    }

    @Transactional
    public void updatePassword(PasswordUpdateRequest req) {
        PasswordResetToken prt = validateResetToken(req.getToken());
        prt.getUser().setPassword(passwordEncoder.encode(req.getNewPassword()));
        userRepository.save(prt.getUser());
        prt.setUsed(true);
        tokenRepository.save(prt);
    }

    private PasswordResetToken validateResetToken(String token) {
        return tokenRepository.findByToken(token)
                .filter(t -> !t.isUsed() && t.getExpiryDate().isAfter(Instant.now()))
                .orElseThrow(() -> new InvalidTokenException(
                        "Lien de réinitialisation invalide ou expiré."));
    }
}

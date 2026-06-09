package com.bloom.authservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Point d'entrée unique pour l'envoi de mails côté auth-service.
 * Encapsule {@link JavaMailSender} et la mise en forme des messages
 * (vérification d'email, réinitialisation de mot de passe).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Value("${spring.mail.username:noreply@bloom.dev}")
    private String fromAddress;

    /**
     * Envoie le mail de vérification à un nouvel utilisateur.
     * @param to    adresse du destinataire
     * @param token UUID à passer à {@code GET /api/auth/verify-email?token=...}
     */
    public void sendVerificationEmail(String to, String token) {
        String link = frontendUrl + "/verify-email?token=" + token;
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(fromAddress);
        msg.setTo(to);
        msg.setSubject("BLOOM — Vérifiez votre adresse email");
        msg.setText("""
                Bienvenue sur BLOOM !

                Pour activer votre compte, merci de cliquer sur le lien ci-dessous
                (valable 24 heures) :

                %s

                Si vous n'êtes pas à l'origine de cette inscription, ignorez ce mail.
                """.formatted(link));
        send(msg, "verification", to);
    }

    /**
     * Envoie le mail de réinitialisation de mot de passe.
     * @param to    adresse du destinataire
     * @param token UUID à passer à {@code POST /api/auth/password-reset/update}
     */
    public void sendPasswordResetEmail(String to, String token) {
        String link = frontendUrl + "/reset-password?token=" + token;
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(fromAddress);
        msg.setTo(to);
        msg.setSubject("BLOOM — Réinitialisation de mot de passe");
        msg.setText("""
                Lien de réinitialisation (valide 1 heure) :

                %s

                Si vous n'avez pas demandé de réinitialisation, ignorez ce mail.
                """.formatted(link));
        send(msg, "password-reset", to);
    }

    private void send(SimpleMailMessage msg, String kind, String to) {
        try {
            mailSender.send(msg);
            log.info("Email envoyé — type={}, destinataire={}", kind, to);
        } catch (MailException e) {
            // On log et on relance : la transaction métier (création user / token)
            // doit pouvoir détecter l'échec et rollback proprement.
            log.error("Échec d'envoi de mail — type={}, destinataire={} : {}", kind, to, e.getMessage());
            throw e;
        }
    }
}

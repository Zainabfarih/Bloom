package com.bloom.authservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailService, "frontendUrl", "http://localhost:5173");
        ReflectionTestUtils.setField(emailService, "fromAddress", "noreply@bloom.dev");
    }

    @Test
    @DisplayName("sendVerificationEmail : envoie un message avec sujet et lien attendus")
    void send_verification_email_format() {
        emailService.sendVerificationEmail("alice@bloom.dev", "verification-token-uuid");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage msg = captor.getValue();

        assertThat(msg.getFrom()).isEqualTo("noreply@bloom.dev");
        assertThat(msg.getTo()).contains("alice@bloom.dev");
        assertThat(msg.getSubject()).contains("Vérifiez");
        assertThat(msg.getText())
                .contains("http://localhost:5173/verify-email?token=verification-token-uuid")
                .contains("24 heures");
    }

    @Test
    @DisplayName("sendPasswordResetEmail : envoie un message avec sujet et lien attendus")
    void send_password_reset_email_format() {
        emailService.sendPasswordResetEmail("alice@bloom.dev", "reset-token-uuid");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage msg = captor.getValue();

        assertThat(msg.getTo()).contains("alice@bloom.dev");
        assertThat(msg.getSubject()).contains("Réinitialisation");
        assertThat(msg.getText())
                .contains("http://localhost:5173/reset-password?token=reset-token-uuid")
                .contains("1 heure");
    }

    @Test
    @DisplayName("Erreur SMTP relancée pour permettre le rollback transactionnel")
    void smtp_failure_propagates() {
        doThrow(new MailSendException("smtp down"))
                .when(mailSender).send(org.mockito.ArgumentMatchers.any(SimpleMailMessage.class));

        assertThatThrownBy(() ->
                emailService.sendVerificationEmail("alice@bloom.dev", "x"))
                .isInstanceOf(MailSendException.class);
    }
}

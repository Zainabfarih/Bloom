package com.bloom.authservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Token à usage unique envoyé par mail à l'inscription pour valider l'adresse.
 * Le compte associé reste {@code emailVerified=false} tant que ce token n'a
 * pas été consommé via {@code GET /api/auth/verify-email?token=...}.
 */
@Entity
@Table(name = "email_verification_tokens")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailVerificationToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;

    @Column(nullable = false)
    private Instant expiryDate;

    @Column(nullable = false)
    @Builder.Default
    private boolean used = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}

package com.bloom.authservice.service;

import com.bloom.authservice.dto.PasswordResetRequest;
import com.bloom.authservice.dto.PasswordUpdateRequest;
import com.bloom.authservice.entity.PasswordResetToken;
import com.bloom.authservice.entity.User;
import com.bloom.authservice.repository.PasswordResetTokenRepository;
import com.bloom.authservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PasswordService {

    @Value("${jwt.password-reset-expiration}")
    private long passwordResetExpiration;

    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void initiatePasswordReset(PasswordResetRequest req) {
        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // supprime les anciens tokens de reset
        passwordResetTokenRepository.deleteByUserId(user.getId());

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(UUID.randomUUID().toString())
                .expiryDate(Instant.now().plusMillis(passwordResetExpiration))
                .used(false)
                .user(user)
                .build();
        passwordResetTokenRepository.save(resetToken);

        // TODO: envoyer le token par email (JavaMailSender)
        System.out.println("Password reset token: " + resetToken.getToken());
    }

    @Transactional
    public void updatePassword(PasswordUpdateRequest req) {
        PasswordResetToken resetToken = validateResetToken(req.getToken());
        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        userRepository.save(user);
        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);
    }

    private PasswordResetToken validateResetToken(String token) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid reset token"));
        if (resetToken.isUsed()) {
            throw new RuntimeException("Reset token already used");
        }
        if (resetToken.getExpiryDate().isBefore(Instant.now())) {
            throw new RuntimeException("Reset token expired");
        }
        return resetToken;
    }
}
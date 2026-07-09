package com.bloom.authservice.controller;

import com.bloom.authservice.dto.*;
import com.bloom.authservice.service.AuthService;
import com.bloom.authservice.service.EmailVerificationService;
import com.bloom.authservice.service.PasswordService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Validated
public class AuthController {

    private final AuthService authService;
    private final PasswordService passwordService;
    private final EmailVerificationService emailVerificationService;

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(req));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
        return ResponseEntity.ok(authService.login(req));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestBody RefreshTokenRequest req) {
        return ResponseEntity.ok(authService.refreshToken(req));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody RefreshTokenRequest req) {
        authService.logout(req.getRefreshToken());
        return ResponseEntity.noContent().build();
    }

    // ─── Vérification d'email ────────────────────────────────────────────────

    /**
     * Endpoint cliqué depuis le mail de vérification.
     * Renvoie 200 si le token est valide et non expiré, 400 sinon.
     */
    @GetMapping("/verify-email")
    public ResponseEntity<Void> verifyEmail(@RequestParam("token") @NotBlank String token) {
        emailVerificationService.verifyEmail(token);
        return ResponseEntity.ok().build();
    }

    /**
     * Permet à l'utilisateur de redemander un mail de vérification.
     * Toujours 200 (anti user-enumeration), même si l'email n'existe pas
     * ou est déjà vérifié.
     */
    @PostMapping("/verify-email/resend")
    public ResponseEntity<Void> resendVerification(@Valid @RequestBody ResendVerificationRequest req) {
        emailVerificationService.resendVerification(req.getEmail());
        return ResponseEntity.ok().build();
    }

    // ─── Reset password ──────────────────────────────────────────────────────

    @PostMapping("/password-reset/initiate")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody PasswordResetRequest req) {
        passwordService.initiatePasswordReset(req);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/password-reset/update")
    public ResponseEntity<Void> updatePassword(@Valid @RequestBody PasswordUpdateRequest req) {
        passwordService.updatePassword(req);
        return ResponseEntity.ok().build();
    }
}

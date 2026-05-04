package com.bloom.authservice.controller;

import com.bloom.authservice.dto.*;
import com.bloom.authservice.service.AuthService;
import com.bloom.authservice.service.PasswordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final PasswordService passwordService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest req) {
        return ResponseEntity.ok(authService.register(req));
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
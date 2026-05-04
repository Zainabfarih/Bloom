package com.bloom.authservice.controller;

import com.bloom.authservice.dto.TokenValidationResponse;
import com.bloom.authservice.service.TokenValidationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tokens")
@RequiredArgsConstructor
public class TokenController {

    private final TokenValidationService tokenValidationService;

    @GetMapping("/validate")
    public ResponseEntity<TokenValidationResponse> validateToken(
            @RequestHeader("Authorization") String bearerToken) {
        String token = bearerToken.substring(7);
        return ResponseEntity.ok(tokenValidationService.validateToken(token));
    }
}
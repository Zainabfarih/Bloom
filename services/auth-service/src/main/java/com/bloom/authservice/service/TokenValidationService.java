package com.bloom.authservice.service;

import com.bloom.authservice.dto.TokenValidationResponse;
import com.bloom.authservice.entity.User;
import com.bloom.authservice.repository.UserRepository;
import com.bloom.authservice.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TokenValidationService {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public TokenValidationResponse validateToken(String token) {
        try {
            String email = jwtService.extractUsername(token);
            User user = userRepository.findByEmail(email).orElse(null);
            if (user == null) {
                return TokenValidationResponse.builder().valid(false).build();
            }
            boolean valid = jwtService.isTokenValid(token, user);
            if (!valid) {
                return TokenValidationResponse.builder().valid(false).build();
            }
            return TokenValidationResponse.builder()
                    .valid(true)
                    .userId(jwtService.extractUserId(token))
                    .email(email)
                    .role(jwtService.extractRole(token))
                    .build();
        } catch (Exception e) {
            return TokenValidationResponse.builder().valid(false).build();
        }
    }
}
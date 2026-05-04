package com.bloom.authservice.service;

import com.bloom.authservice.dto.TokenValidationResponse;
import com.bloom.authservice.repository.UserRepository;
import com.bloom.authservice.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TokenValidationService {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public TokenValidationResponse validateToken(String token) {
        try {
            String email = jwtService.extractUsername(token);
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);
            boolean valid = jwtService.isTokenValid(token, userDetails);
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
package com.bloom.authservice.service;

import com.bloom.authservice.dto.TokenValidationResponse;
import com.bloom.authservice.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j //Adds the 'log' object automatically
public class TokenValidationService {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public TokenValidationResponse validateToken(String token) {
        try {
            String email = jwtService.extractUsername(token);
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);

            if (jwtService.isTokenValid(token, userDetails)) {
                return TokenValidationResponse.builder()
                        .valid(true)
                        .userId(jwtService.extractUserId(token))
                        .email(email)
                        .role(jwtService.extractRole(token))
                        .build();
            }
        } catch (Exception e) {
            log.error("Token validation failed: {}", e.getMessage());
        }
        
        return TokenValidationResponse.builder().valid(false).build();
    }
}
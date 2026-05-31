package com.bloom.authservice.service;

import com.bloom.authservice.entity.Role;
import com.bloom.authservice.entity.User;
import com.bloom.authservice.dto.*;
import com.bloom.authservice.repository.UserRepository;
import com.bloom.authservice.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final AuthenticationManager authenticationManager;

    @Value("${app.max-failed-login-attempts}")
    private int maxFailedAttempts;

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new IllegalArgumentException("Email already in use");
        }
        User user = userRepository.save(User.builder()
                .firstName(req.getFirstName())
                .lastName(req.getLastName())
                .email(req.getEmail())
                .password(passwordEncoder.encode(req.getPassword()))
                .role(Role.STUDENT)
                .build());

        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        if (user.isLocked()) {
            throw new LockedException("Account locked due to too many failed attempts");
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword()));
        } catch (BadCredentialsException e) {
            recordFailedLogin(req.getEmail());
            throw e;
        }

        // Reset failed attempts on success
        user.setFailedLoginAttempts(0);
        userRepository.save(user);

        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest req) {
        return refreshTokenService.findByToken(req.getRefreshToken())
                .map(refreshTokenService::verifyExpiration)
                .map(rt -> buildAuthResponse(rt.getUser()))
                .orElseThrow(() -> new RuntimeException("Refresh token not found"));
    }

    @Transactional
    public void logout(String refreshToken) {
        refreshTokenService.revokeToken(refreshToken);
    }

    @Transactional
    public void recordFailedLogin(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
            if (user.getFailedLoginAttempts() >= maxFailedAttempts) {
                user.setLocked(true);
            }
            userRepository.save(user);
        });
    }

    private AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        var refreshToken = refreshTokenService.createRefreshToken(user.getId());
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .tokenType("Bearer")
                .user(UserDTO.builder()
                        .id(user.getId())
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .email(user.getEmail())
                        .role(user.getRole().name())
                        .build())
                .build();
    }
}
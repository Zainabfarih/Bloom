package com.bloom.authservice.service;

import com.bloom.authservice.dto.*;
import com.bloom.authservice.entity.RefreshToken;
import com.bloom.authservice.entity.User;
import com.bloom.authservice.entity.Role;
import com.bloom.authservice.repository.UserRepository;
import com.bloom.authservice.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final AuthenticationManager authenticationManager;

    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new RuntimeException("Email already in use");
        }
        User user = User.builder()
                .firstName(req.getFirstName())
                .lastName(req.getLastName())
                .email(req.getEmail())
                .password(passwordEncoder.encode(req.getPassword()))
                .role(Role.STUDENT)
                .enabled(true)
                .locked(false)
                .failedLoginAttempts(0)
                .build();
        userRepository.save(user);
        return buildAuthResponse(user);
    }

    public AuthResponse login(LoginRequest req) {
        System.out.println("GETTING MY RESPONSE . . .");
        System.out.println(req.getEmail());
        System.out.println(req.getPassword());
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword())
        );
        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setFailedLoginAttempts(0);
        userRepository.save(user);
        return buildAuthResponse(user);
    }

    public AuthResponse refreshToken(RefreshTokenRequest req) {
        RefreshToken refreshToken = refreshTokenService.findByToken(req.getRefreshToken())
                .orElseThrow(() -> new RuntimeException("Refresh token not found"));
        refreshTokenService.verifyExpiration(refreshToken);
        User user = refreshToken.getUser();
        return buildAuthResponse(user);
    }

    public void logout(String token) {
        refreshTokenService.revokeToken(token);
    }

    public void recordFailedLogin(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
            if (user.getFailedLoginAttempts() >= 5) {
                user.setLocked(true);
            }
            userRepository.save(user);
        });
    }

    private AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getId());
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
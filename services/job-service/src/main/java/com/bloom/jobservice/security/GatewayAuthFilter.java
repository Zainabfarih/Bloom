package com.bloom.jobservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.util.HexFormat;
import java.util.List;


/**
 * Authentifie les requêtes : via la Gateway (headers X-User-* + secret partagé)
 * ou en appel direct service-à-service (Bearer JWT). Stateless, sans session.
 */
@Component
@Slf4j
public class GatewayAuthFilter extends OncePerRequestFilter {

    @Value("${internal.security.gateway-secret}")
    private String expectedSecret;

    private final SecretKey jwtKey;

    public GatewayAuthFilter(@Value("${jwt.secret}") String jwtSecret) {
        this.jwtKey = Keys.hmacShaKeyFor(HexFormat.of().parseHex(jwtSecret.trim()));
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain)
            throws ServletException, IOException {

        String gatewaySecretHeader = request.getHeader("X-Gateway-Secret");
        String userIdHeader        = request.getHeader("X-User-Id");
        String roleHeader          = request.getHeader("X-User-Role");
        String authorizationHeader = request.getHeader("Authorization");

        // Scénario 1 : requête arrivée via la Gateway
        if (userIdHeader != null) {

            if (gatewaySecretHeader == null || !gatewaySecretHeader.trim().equals(expectedSecret)) {
                log.warn("Usurpation d'identité ou contournement de la Gateway détecté ! Chemin: {}",
                        request.getRequestURI());
                response.sendError(HttpServletResponse.SC_FORBIDDEN,
                        "Direct access to this microservice is forbidden.");
                return;
            }

            try {
                authenticate(Long.valueOf(userIdHeader), parseRole(roleHeader), request);
                log.debug("Gateway auth validée — userId={}, role={}", userIdHeader, roleHeader);
            } catch (NumberFormatException e) {
                log.warn("Format de header X-User-Id invalide: {}", userIdHeader);
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid user identifier format.");
                return;
            }

        // Scénario 2 : appel direct service-à-service avec Bearer token
        } else if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            try {
                Claims claims = parseClaims(authorizationHeader.substring(7));

                Long userId = Long.valueOf(claims.get("userId", String.class));

                String role = claims.get("role", String.class);

                authenticate(userId, parseRole(role), request);
                log.debug("JWT direct validé — userId={}, role={}", userId, role);

            } catch (Exception e) {
                log.debug("JWT direct invalide pour {} : {}", request.getRequestURI(), e.getMessage());
            }
        }

        chain.doFilter(request, response);
    }

    private void authenticate(Long userId, List<SimpleGrantedAuthority> authorities,
                              HttpServletRequest request) {
        var auth = new UsernamePasswordAuthenticationToken(userId, null, authorities);
        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private List<SimpleGrantedAuthority> parseRole(String role) {
        return (role != null && !role.isBlank())
                ? List.of(new SimpleGrantedAuthority("ROLE_" + role.trim()))
                : List.of();
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(jwtKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}

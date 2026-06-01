package com.bloom.apigateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;

@Component
@Slf4j
public class JwtGatewayFilter extends OncePerRequestFilter {

    private final SecretKey key;

    private final String internalSecret;

    private static final Set<String> EXCLUDED_PATHS = Set.of(
            "/api/auth/login",
            "/api/auth/register",
            "/actuator/health",
            "/fallback/job",
            "/fallback/auth",
            "/fallback/cv"
    );

    public JwtGatewayFilter(@Value("${jwt.secret}") String secret,
                            @Value("${internal.security.gateway-secret}") String internalSecret) {
        byte[] keyBytes = HexFormat.of().parseHex(secret.trim());
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.internalSecret = internalSecret;
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getRequestURI();
        return EXCLUDED_PATHS.contains(path) || path.startsWith("/actuator/");
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer ")) {
            log.debug("Missing Authorization header for path: {}", request.getRequestURI());
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "Missing Authorization header");
            return;
        }

        try {
            String token = header.substring(7);
            Claims claims = parseClaims(token);

            String userId = claims.getSubject();
            List<?> roles = claims.get("roles", List.class);
            String rolesStr = roles != null ? String.join(",", roles.stream()
                    .map(Object::toString)
                    .toList()) : "";

            MutableHttpServletRequest mutableRequest = new MutableHttpServletRequest(request);
            mutableRequest.putHeader("X-User-Id", userId);
            mutableRequest.putHeader("X-User-Roles", rolesStr);

            mutableRequest.putHeader("X-Gateway-Secret", internalSecret);

            log.debug("JWT valid — userId={}, roles={}, path={}",
                    userId, rolesStr, request.getRequestURI());

            chain.doFilter(mutableRequest, response);

        } catch (ExpiredJwtException e) {
            log.debug("JWT expired for path: {}", request.getRequestURI());
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "Token expired");
        } catch (JwtException e) {
            log.debug("Invalid JWT for path: {}", request.getRequestURI());
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "Invalid token");
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}

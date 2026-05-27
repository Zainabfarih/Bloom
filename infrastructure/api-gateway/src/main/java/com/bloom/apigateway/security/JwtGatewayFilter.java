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
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

/**
 * JwtGatewayFilter — valide le JWT UNE SEULE FOIS au niveau du gateway.
 *
 * Ce que ce filtre fait :
 *   1. Vérifie si la route est publique (login, register, health...) → laisse passer
 *   2. Vérifie la présence du header Authorization: Bearer <token>
 *   3. Valide la signature et l'expiration du JWT
 *   4. Extrait userId et roles depuis les claims
 *   5. Ajoute les headers X-User-Id et X-User-Roles → les services downstream
 *      peuvent lire ces headers sans re-valider le JWT
 *
 * Ce que ce filtre NE fait PAS :
 *   - Il ne gère pas l'autorisation (ADMIN vs USER) → délégué aux services
 *   - Il ne crée pas de session HTTP (stateless)
 *
 * IMPORTANT : job-service a déjà son propre JwtAuthenticationFilter.
 * Avec ce filtre au niveau gateway, job-service peut soit :
 *   a) Garder son filtre (double sécurité, recommandé) et lire X-User-Id depuis le header
 *   b) Faire confiance au gateway et lire directement X-User-Id (simplification)
 * Pour ce projet, on garde les deux (défense en profondeur).
 */
@Component
@Slf4j
public class JwtGatewayFilter extends OncePerRequestFilter {

    private final SecretKey key;

    /**
     * Routes qui ne nécessitent PAS de JWT.
     * Ajuste selon tes endpoints publics.
     */
    private static final Set<String> EXCLUDED_PATHS = Set.of(
            "/api/auth/login",
            "/api/auth/register",
            "/actuator/health",
            "/fallback/job",
            "/fallback/auth",
            "/fallback/cv"
    );

    public JwtGatewayFilter(@Value("${jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getRequestURI();
        // Exclure les paths publics et tout ce qui commence par /actuator/
        return EXCLUDED_PATHS.contains(path) || path.startsWith("/actuator/");
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        // Pas de token → 401
        if (header == null || !header.startsWith("Bearer ")) {
            log.debug("Missing Authorization header for path: {}", request.getRequestURI());
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "Missing Authorization header");
            return;
        }

        try {
            String token = header.substring(7);
            Claims claims = parseClaims(token);

            // Extraire userId et roles
            String userId = claims.getSubject();
            List<?> roles = claims.get("roles", List.class);
            String rolesStr = roles != null ? String.join(",", roles.stream()
                    .map(Object::toString)
                    .toList()) : "";

            // Ajouter les headers pour les services downstream
            // job-service peut lire X-User-Id au lieu de re-parser le JWT
            MutableHttpServletRequest mutableRequest =
                    new MutableHttpServletRequest(request);
            mutableRequest.putHeader("X-User-Id", userId);
            mutableRequest.putHeader("X-User-Roles", rolesStr);

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
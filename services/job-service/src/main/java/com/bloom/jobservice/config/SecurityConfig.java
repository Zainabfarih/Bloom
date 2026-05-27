package com.bloom.jobservice.config;

import com.bloom.jobservice.security.GatewayAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * SecurityConfig — stateless, basé sur les headers X-User-Id / X-User-Roles
 * injectés par l'API Gateway (JwtGatewayFilter).
 *
 * ══════════════════════════════════════════════════════════════════
 * TESTER SANS USER-SERVICE (user-service pas encore déployé)
 * ══════════════════════════════════════════════════════════════════
 *
 * Le service NE valide PAS le JWT lui-même — c'est le rôle de l'API Gateway.
 * Le job-service lit uniquement les headers X-User-Id et X-User-Roles.
 *
 * Pour simuler un utilisateur authentifié en DEV/TEST, il suffit d'appeler
 * directement le job-service (port 8083) EN BYPAS de la gateway, avec :
 *
 *   curl http://localhost:8083/api/job/saved \
 *        -H "X-User-Id: 1" \
 *        -H "X-User-Roles: STUDENT"
 *
 * Pour un admin :
 *   curl -X DELETE "http://localhost:8083/api/job/admin/cache?query=java" \
 *        -H "X-User-Id: 99" \
 *        -H "X-User-Roles: ADMIN"
 *
 * L'endpoint Authorization: Bearer xxx n'est pas vérifié par job-service,
 * mais est transmis tel quel à cv-service via Feign pour le skill matching.
 * En dev, passez n'importe quelle valeur : -H "Authorization: Bearer test".
 *
 * ENDPOINTS PUBLICS (pas de headers requis) :
 *   - GET /api/job/search?query=...
 *   - GET /api/job/{jobId}
 *   - GET /actuator/health
 *   - GET /swagger-ui/**
 * ══════════════════════════════════════════════════════════════════
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final GatewayAuthFilter gatewayAuthFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Infrastructure
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/actuator").permitAll()
                        .requestMatchers("/actuator/metrics").permitAll()
                        .requestMatchers("/actuator/loggers").permitAll()
                        // Swagger
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        // Jobs publics (lecture seule, sans authentification)
                        .requestMatchers("/api/job/search").permitAll()
                        .requestMatchers("/api/job/{jobId}").permitAll()
                        // Tout le reste nécessite X-User-Id (injecté par gateway)
                        .anyRequest().authenticated()
                )
                .addFilterBefore(gatewayAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}

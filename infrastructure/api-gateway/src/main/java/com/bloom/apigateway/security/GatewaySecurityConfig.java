package com.bloom.apigateway.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * GatewaySecurityConfig — configure Spring Security au niveau du gateway.
 *
 * RÔLE DE CE FICHIER :
 * Spring Security, quand il est sur le classpath, protège TOUT par défaut avec
 * un login form. Sans cette config → le gateway bloquerait toutes les requêtes.
 * On désactive ce comportement par défaut et on branche notre JwtGatewayFilter.
 *
 * LOGIQUE :
 *   - Toutes les requêtes passent par JwtGatewayFilter (sauf les excluded paths)
 *   - Spring Security lui-même laisse tout passer (.anyRequest().permitAll())
 *   - C'est JwtGatewayFilter qui retourne le 401 si le JWT est absent/invalide
 *   - L'autorisation (ADMIN vs USER) est gérée par les services individuels
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class GatewaySecurityConfig {

    private final JwtGatewayFilter jwtGatewayFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                // Désactiver CSRF — API REST stateless, pas de formulaires
                .csrf(AbstractHttpConfigurer::disable)

                // Pas de session HTTP — chaque requête porte son JWT
                .sessionManagement(s ->
                        s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Désactiver le login form par défaut de Spring Security
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)

                // Spring Security laisse tout passer — c'est JwtGatewayFilter
                // qui gère le 401. On ne duplique pas la logique ici.
                .authorizeHttpRequests(auth ->
                        auth.anyRequest().permitAll())

                // Brancher notre filtre JWT AVANT le filtre d'auth Spring Security
                .addFilterBefore(jwtGatewayFilter,
                        UsernamePasswordAuthenticationFilter.class)

                .build();
    }
}
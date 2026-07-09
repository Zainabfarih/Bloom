package com.bloom.roadmapservice.security;

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

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Component
@Slf4j
public class GatewayAuthFilter extends OncePerRequestFilter {

    @Value("${internal.security.gateway-secret}")
    private String expectedSecret;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain)
            throws ServletException, IOException {

        String gatewaySecretHeader = request.getHeader("X-Gateway-Secret");
        String userIdHeader        = request.getHeader("X-User-Id");
        String rolesHeader         = request.getHeader("X-User-Roles");

        if (userIdHeader != null) {
            if (gatewaySecretHeader == null || !gatewaySecretHeader.trim().equals(expectedSecret)) {
                log.warn("Gateway bypass attempt — path: {}", request.getRequestURI());
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Direct access forbidden.");
                return;
            }
            try {
                Long userId = Long.valueOf(userIdHeader);
                List<SimpleGrantedAuthority> authorities = (rolesHeader != null && !rolesHeader.isBlank())
                        ? Arrays.stream(rolesHeader.split(","))
                          .map(r -> new SimpleGrantedAuthority("ROLE_" + r.trim()))
                          .toList()
                        : List.of();
                var auth = new UsernamePasswordAuthenticationToken(userId, null, authorities);
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (NumberFormatException e) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid X-User-Id.");
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
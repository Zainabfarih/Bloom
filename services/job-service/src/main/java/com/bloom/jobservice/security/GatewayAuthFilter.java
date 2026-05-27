package com.bloom.jobservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
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

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain)
            throws ServletException, IOException {

        String userIdHeader = request.getHeader("X-User-Id");
        String rolesHeader  = request.getHeader("X-User-Roles");

        if (userIdHeader != null) {
            try {
                Long userId = Long.valueOf(userIdHeader);

                List<SimpleGrantedAuthority> authorities = (rolesHeader != null && !rolesHeader.isBlank())
                        ? Arrays.stream(rolesHeader.split(","))
                        .map(r -> new SimpleGrantedAuthority("ROLE_" + r.trim()))
                        .toList()
                        : List.of();

                var auth = new UsernamePasswordAuthenticationToken(
                        userId,      // ← auth.getPrincipal() dans le controller = Long userId ✅
                        null,
                        authorities
                );
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);

                log.debug("Gateway auth — userId={}, roles={}", userId, rolesHeader);

            } catch (NumberFormatException e) {
                log.warn("Invalid X-User-Id header: {}", userIdHeader);
            }
        }

        chain.doFilter(request, response);
    }
}
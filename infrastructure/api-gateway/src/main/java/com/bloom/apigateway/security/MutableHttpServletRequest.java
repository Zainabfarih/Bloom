package com.bloom.apigateway.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.util.*;

/**
 * MutableHttpServletRequest — permet d'ajouter des headers à la requête HTTP.
 *
 * Par défaut, HttpServletRequest est immuable (on ne peut pas ajouter de headers).
 * Ce wrapper permet au JwtGatewayFilter d'ajouter X-User-Id et X-User-Roles
 * APRÈS validation du JWT, pour que les services downstream puissent les lire
 * sans re-parser le token.
 *
 * Exemple d'utilisation dans job-service (optionnel) :
 *   String userId = request.getHeader("X-User-Id");  // plus simple que parser le JWT
 */
public class MutableHttpServletRequest extends HttpServletRequestWrapper {

    private final Map<String, String> customHeaders = new HashMap<>();

    public MutableHttpServletRequest(HttpServletRequest request) {
        super(request);
    }

    public void putHeader(String name, String value) {
        customHeaders.put(name, value);
    }

    @Override
    public String getHeader(String name) {
        // Chercher d'abord dans les headers custom
        String value = customHeaders.get(name);
        if (value != null) return value;
        return super.getHeader(name);
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        String value = customHeaders.get(name);
        if (value != null) {
            return Collections.enumeration(List.of(value));
        }
        return super.getHeaders(name);
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        Set<String> names = new HashSet<>(customHeaders.keySet());
        Enumeration<String> original = super.getHeaderNames();
        while (original.hasMoreElements()) {
            names.add(original.nextElement());
        }
        return Collections.enumeration(names);
    }
}
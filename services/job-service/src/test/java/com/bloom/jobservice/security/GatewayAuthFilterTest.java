package com.bloom.jobservice.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

class GatewayAuthFilterTest {

    private static final String GATEWAY_SECRET = "test-gateway-secret";
    private static final String JWT_SECRET =
            "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";

    private GatewayAuthFilter filter;
    private SecretKey jwtKey;

    @BeforeEach
    void setUp() {
        filter = new GatewayAuthFilter(JWT_SECRET);
        ReflectionTestUtils.setField(filter, "expectedSecret", GATEWAY_SECRET);
        jwtKey = Keys.hmacShaKeyFor(HexFormat.of().parseHex(JWT_SECRET));
        SecurityContextHolder.clearContext();
    }

    // ─── Scénario 1 : via Gateway ────────────────────────────────────────────

    @Test
    @DisplayName("X-User-Id + X-User-Role → Authentication créée (cas principal après Gateway)")
    void creates_auth_from_gateway_headers() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Gateway-Secret", GATEWAY_SECRET);
        request.addHeader("X-User-Id", "42");
        request.addHeader("X-User-Role", "STUDENT");

        filter.doFilterInternal(request, new MockHttpServletResponse(), new MockFilterChain());

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.isAuthenticated()).isTrue();
        assertThat(auth.getPrincipal()).isEqualTo(42L);
        assertThat(auth.getAuthorities())
                .extracting(Object::toString)
                .containsExactly("ROLE_STUDENT");
    }

    @Test
    @DisplayName("X-User-Id sans secret Gateway valide → 403, contexte vide")
    void rejects_when_gateway_secret_missing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-User-Id", "42");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("X-User-Id présent, X-User-Role absent → authentifié sans authority")
    void authenticates_without_role_header() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Gateway-Secret", GATEWAY_SECRET);
        request.addHeader("X-User-Id", "7");

        filter.doFilterInternal(request, new MockHttpServletResponse(), new MockFilterChain());

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getPrincipal()).isEqualTo(7L);
        assertThat(auth.getAuthorities()).isEmpty();
    }

    @Test
    @DisplayName("X-User-Role vide/blank → authentifié sans authority")
    void handles_blank_role_header() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Gateway-Secret", GATEWAY_SECRET);
        request.addHeader("X-User-Id", "5");
        request.addHeader("X-User-Role", "   ");

        filter.doFilterInternal(request, new MockHttpServletResponse(), new MockFilterChain());

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getAuthorities()).isEmpty();
    }

    @Test
    @DisplayName("X-User-Id non numérique (secret valide) → 400, pas d'exception, contexte vide")
    void handles_non_numeric_user_id_gracefully() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Gateway-Secret", GATEWAY_SECRET);
        request.addHeader("X-User-Id", "not-a-number");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThatNoException().isThrownBy(() ->
                filter.doFilterInternal(request, response, new MockFilterChain()));

        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    // ─── Scénario 2 : appel direct service-à-service avec Bearer ───────────────

    @Test
    @DisplayName("Bearer JWT valide (appel direct Feign) → Authentication créée")
    void creates_auth_from_direct_bearer_token() throws Exception {
        String token = Jwts.builder()
                .subject("student@example.com")
                .claim("userId", "7")
                .claim("role", "STUDENT")
                .signWith(jwtKey)
                .compact();

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);

        filter.doFilterInternal(request, new MockHttpServletResponse(), new MockFilterChain());

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getPrincipal()).isEqualTo(7L);
        assertThat(auth.getAuthorities())
                .extracting(Object::toString)
                .containsExactly("ROLE_STUDENT");
    }

    @Test
    @DisplayName("Bearer JWT invalide → contexte vide, pas d'exception (401 délégué à Spring Security)")
    void invalid_bearer_leaves_context_empty() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer not-a-real-token");

        assertThatNoException().isThrownBy(() ->
                filter.doFilterInternal(request, new MockHttpServletResponse(), new MockFilterChain()));

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    // ─── Aucun header ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("Aucun header → Authentication null (requête non passée par Gateway ou /actuator)")
    void no_headers_no_auth() throws Exception {
        filter.doFilterInternal(
                new MockHttpServletRequest(),
                new MockHttpServletResponse(),
                new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("La chaîne de filtres est toujours appelée (même sans headers)")
    void always_calls_filter_chain() throws Exception {
        MockFilterChain chain = new MockFilterChain();

        filter.doFilterInternal(
                new MockHttpServletRequest(),
                new MockHttpServletResponse(),
                chain);

        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    @DisplayName("La chaîne est appelée même avec authentification valide")
    void calls_chain_after_setting_auth() throws Exception {
        MockFilterChain chain = new MockFilterChain();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Gateway-Secret", GATEWAY_SECRET);
        request.addHeader("X-User-Id", "1");
        request.addHeader("X-User-Role", "STUDENT");

        filter.doFilterInternal(request, new MockHttpServletResponse(), chain);

        assertThat(chain.getRequest()).isNotNull();
    }
}

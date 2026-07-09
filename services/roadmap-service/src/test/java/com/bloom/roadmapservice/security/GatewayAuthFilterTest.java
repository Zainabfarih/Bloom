package com.bloom.roadmapservice.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayAuthFilterTest {

    private static final String GATEWAY_SECRET = "test-gateway-secret";

    private GatewayAuthFilter filter;

    @BeforeEach
    void setUp() {
        filter = new GatewayAuthFilter();
        ReflectionTestUtils.setField(filter, "expectedSecret", GATEWAY_SECRET);
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Headers Gateway valides + plusieurs rôles → Authentication créée avec les autorités")
    void valid_headers_create_authentication_with_roles() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Gateway-Secret", GATEWAY_SECRET);
        request.addHeader("X-User-Id", "42");
        request.addHeader("X-User-Roles", "STUDENT,USER");

        filter.doFilterInternal(request, new MockHttpServletResponse(), new MockFilterChain());

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getPrincipal()).isEqualTo(42L);
        assertThat(auth.getAuthorities()).extracting(Object::toString)
                .containsExactly("ROLE_STUDENT", "ROLE_USER");
    }

    @Test
    @DisplayName("X-User-Id sans X-User-Roles → autorités vides")
    void missing_roles_creates_empty_authorities() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Gateway-Secret", GATEWAY_SECRET);
        request.addHeader("X-User-Id", "1");

        filter.doFilterInternal(request, new MockHttpServletResponse(), new MockFilterChain());

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getAuthorities()).isEmpty();
    }

    @Test
    @DisplayName("X-User-Id sans secret Gateway → 403, contexte vide")
    void missing_gateway_secret_returns_403() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-User-Id", "42");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("Secret Gateway invalide → 403")
    void wrong_gateway_secret_returns_403() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-User-Id", "42");
        request.addHeader("X-Gateway-Secret", "wrong-secret");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(403);
    }

    @Test
    @DisplayName("X-User-Id non numérique → 400")
    void non_numeric_user_id_returns_400() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Gateway-Secret", GATEWAY_SECRET);
        request.addHeader("X-User-Id", "not-a-long");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(400);
    }

    @Test
    @DisplayName("Aucun X-User-Id → la chaîne continue, contexte vide")
    void no_user_id_continues_chain() throws Exception {
        MockFilterChain chain = new MockFilterChain();

        filter.doFilterInternal(new MockHttpServletRequest(), new MockHttpServletResponse(), chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.getRequest()).isNotNull();
    }
}

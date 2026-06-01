package com.bloom.jobservice.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;


class GatewayAuthFilterTest {

    private GatewayAuthFilter filter;

    @BeforeEach
    void setUp() {
        filter = new GatewayAuthFilter();
        SecurityContextHolder.clearContext();
    }


    @Test
    @DisplayName("X-User-Id + X-User-Roles → Authentication créée (cas principal après Gateway)")
    void filter_creates_authentication_when_both_headers_present() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-User-Id", "42");
        request.addHeader("X-User-Roles", "STUDENT");

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
    @DisplayName("Rôles multiples séparés par virgule → toutes les authorities créées")
    void filter_parses_multiple_roles_from_comma_separated_header() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-User-Id", "1");
        request.addHeader("X-User-Roles", "STUDENT,ADMIN");

        filter.doFilterInternal(request, new MockHttpServletResponse(), new MockFilterChain());

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth.getAuthorities())
                .extracting(Object::toString)
                .containsExactlyInAnyOrder("ROLE_STUDENT", "ROLE_ADMIN");
    }

    @Test
    @DisplayName("X-User-Id présent, X-User-Roles absent → authentifié sans authority")
    void filter_authenticates_without_roles_header() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-User-Id", "7");

        filter.doFilterInternal(request, new MockHttpServletResponse(), new MockFilterChain());

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getPrincipal()).isEqualTo(7L);
        assertThat(auth.getAuthorities()).isEmpty();
    }


    @Test
    @DisplayName("Aucun header → Authentication null (requête non passée par Gateway ou /actuator)")
    void filter_does_not_set_auth_when_no_headers() throws Exception {
        filter.doFilterInternal(
                new MockHttpServletRequest(),
                new MockHttpServletResponse(),
                new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }


    @Test
    @DisplayName("X-User-Id non numérique → pas d'erreur, contexte vide (NumberFormatException absorbée)")
    void filter_handles_non_numeric_user_id_gracefully() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-User-Id", "not-a-number");

        assertThatNoException().isThrownBy(() ->
                filter.doFilterInternal(request,
                        new MockHttpServletResponse(),
                        new MockFilterChain())
        );

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("X-User-Roles vide/blank → authentifié sans authority")
    void filter_handles_blank_roles_header() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-User-Id", "5");
        request.addHeader("X-User-Roles", "   ");

        filter.doFilterInternal(request, new MockHttpServletResponse(), new MockFilterChain());

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getAuthorities()).isEmpty();
    }


    @Test
    @DisplayName("La chaîne de filtres est toujours appelée (même sans headers)")
    void filter_always_calls_filter_chain() throws Exception {
        MockFilterChain chain = new MockFilterChain();

        filter.doFilterInternal(
                new MockHttpServletRequest(),
                new MockHttpServletResponse(),
                chain);

        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    @DisplayName("La chaîne est appelée même avec authentification valide")
    void filter_calls_chain_after_setting_auth() throws Exception {
        MockFilterChain chain = new MockFilterChain();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-User-Id", "1");
        request.addHeader("X-User-Roles", "STUDENT");

        filter.doFilterInternal(request, new MockHttpServletResponse(), chain);

        assertThat(chain.getRequest()).isNotNull();
    }
}

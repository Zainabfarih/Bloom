package com.bloom.authservice.security;

import com.bloom.authservice.entity.Role;
import com.bloom.authservice.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private UserDetailsService userDetailsService;

    @InjectMocks
    private JwtAuthFilter filter;

    private User user;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        user = User.builder()
                .id(1L)
                .email("alice@bloom.dev")
                .password("h")
                .firstName("Alice")
                .lastName("Martin")
                .role(Role.STUDENT)
                .build();
    }

    @Test
    @DisplayName("Pas de header Authorization → la chaîne continue, pas d'auth")
    void no_authorization_header_passes_through() throws Exception {
        MockFilterChain chain = new MockFilterChain();

        filter.doFilterInternal(new MockHttpServletRequest(), new MockHttpServletResponse(), chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.getRequest()).isNotNull();
        verifyNoInteractions(jwtService, userDetailsService);
    }

    @Test
    @DisplayName("Header sans préfixe Bearer → la chaîne continue, pas d'auth")
    void non_bearer_authorization_header_ignored() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Basic dXNlcjpwYXNz");

        filter.doFilterInternal(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(jwtService, userDetailsService);
    }

    @Test
    @DisplayName("Bearer JWT valide → Authentication créée")
    void valid_bearer_creates_authentication() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer fake-jwt");

        when(jwtService.extractUsername("fake-jwt")).thenReturn("alice@bloom.dev");
        when(userDetailsService.loadUserByUsername("alice@bloom.dev")).thenReturn(user);
        when(jwtService.isTokenValid("fake-jwt", user)).thenReturn(true);

        filter.doFilterInternal(request, new MockHttpServletResponse(), new MockFilterChain());

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getPrincipal()).isEqualTo(user);
        assertThat(auth.getAuthorities()).extracting(Object::toString)
                .containsExactly("ROLE_STUDENT");
    }

    @Test
    @DisplayName("Bearer JWT invalide → contexte vide, aucune exception remontée")
    void invalid_bearer_does_not_throw() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer broken");

        when(jwtService.extractUsername("broken"))
                .thenThrow(new io.jsonwebtoken.MalformedJwtException("bad token"));

        assertThatNoException().isThrownBy(() ->
                filter.doFilterInternal(request, new MockHttpServletResponse(), new MockFilterChain()));

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("Bearer JWT non valide (isTokenValid=false) → contexte vide")
    void bearer_with_invalid_token_skips_auth() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer expired");

        when(jwtService.extractUsername("expired")).thenReturn("alice@bloom.dev");
        when(userDetailsService.loadUserByUsername("alice@bloom.dev")).thenReturn(user);
        when(jwtService.isTokenValid("expired", user)).thenReturn(false);

        filter.doFilterInternal(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}

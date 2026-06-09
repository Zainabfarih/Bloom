package com.bloom.authservice.service;

import com.bloom.authservice.dto.AdminStatsResponse;
import com.bloom.authservice.dto.UserDTO;
import com.bloom.authservice.entity.Role;
import com.bloom.authservice.entity.User;
import com.bloom.authservice.mapper.UserMapper;
import com.bloom.authservice.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private UserMapper userMapper;

    @InjectMocks
    private UserService userService;

    private User student;
    private User admin;

    @BeforeEach
    void setUp() {
        student = User.builder()
                .id(1L).email("alice@bloom.dev").password("h")
                .firstName("Alice").lastName("Martin").role(Role.STUDENT)
                .createdAt(LocalDateTime.now()).build();
        admin = User.builder()
                .id(99L).email("admin@bloom.dev").password("h")
                .firstName("Admin").lastName("Bloom").role(Role.ADMIN)
                .createdAt(LocalDateTime.now()).build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(User user) {
        var auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    // ─── getUserById ───────────────────────────────────────────────────

    @Test
    @DisplayName("getUserById : succès — propre profil")
    void getUserById_returns_own_profile() {
        authenticateAs(student);
        when(userRepository.findById(1L)).thenReturn(Optional.of(student));
        UserDTO dto = UserDTO.builder().id(1L).email("alice@bloom.dev").build();
        when(userMapper.toDTO(student)).thenReturn(dto);

        assertThat(userService.getUserById(1L)).isEqualTo(dto);
    }

    @Test
    @DisplayName("getUserById : refuse si autre user (non admin) → AccessDeniedException")
    void getUserById_throws_when_other_user_and_not_admin() {
        authenticateAs(student);

        assertThatThrownBy(() -> userService.getUserById(2L))
                .isInstanceOf(AccessDeniedException.class);

        verify(userRepository, never()).findById(any());
    }

    @Test
    @DisplayName("getUserById : ADMIN peut accéder à tout user")
    void getUserById_allows_admin() {
        authenticateAs(admin);
        when(userRepository.findById(1L)).thenReturn(Optional.of(student));
        UserDTO dto = UserDTO.builder().id(1L).build();
        when(userMapper.toDTO(student)).thenReturn(dto);

        assertThat(userService.getUserById(1L)).isEqualTo(dto);
    }

    @Test
    @DisplayName("getUserById : compte désactivé → RuntimeException")
    void getUserById_throws_when_user_disabled() {
        authenticateAs(student);
        student.setEnabled(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(student));

        assertThatThrownBy(() -> userService.getUserById(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("deleted");
    }

    @Test
    @DisplayName("getUserById : id introuvable → RuntimeException")
    void getUserById_throws_when_id_unknown() {
        authenticateAs(student);
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");
    }

    // ─── updateUser ────────────────────────────────────────────────────

    @Test
    @DisplayName("updateUser : succès — met à jour les champs et persiste")
    void updateUser_updates_fields() {
        authenticateAs(student);
        when(userRepository.findById(1L)).thenReturn(Optional.of(student));
        when(userRepository.save(student)).thenReturn(student);
        UserDTO dto = UserDTO.builder().firstName("New").lastName("Name").email("new@bloom.dev").build();
        UserDTO mapped = UserDTO.builder().id(1L).email("new@bloom.dev").build();
        when(userMapper.toDTO(student)).thenReturn(mapped);

        UserDTO result = userService.updateUser(1L, dto);

        assertThat(result).isEqualTo(mapped);
        assertThat(student.getFirstName()).isEqualTo("New");
        assertThat(student.getEmail()).isEqualTo("new@bloom.dev");
    }

    // ─── softDeleteUser ────────────────────────────────────────────────

    @Test
    @DisplayName("softDeleteUser : marque deleted/locked, révoque les refresh tokens")
    void softDelete_marks_deleted_and_revokes_tokens() {
        authenticateAs(student);
        when(userRepository.findById(1L)).thenReturn(Optional.of(student));

        userService.softDeleteUser(1L);

        assertThat(student.isDeleted()).isTrue();
        assertThat(student.isLocked()).isTrue();
        assertThat(student.isEnabled()).isFalse();
        verify(refreshTokenService).revokeByUserId(1L);
        verify(userRepository).save(student);
    }

    // ─── recoverUser ───────────────────────────────────────────────────

    @Test
    @DisplayName("recoverUser : succès — réactive un compte soft-deleted")
    void recoverUser_restores_soft_deleted_user() {
        student.setDeleted(true);
        student.setEnabled(false);
        student.setLocked(true);
        student.setFailedLoginAttempts(8);

        when(userRepository.findByIdIncludingDeleted(1L)).thenReturn(Optional.of(student));

        userService.recoverUser(1L);

        assertThat(student.isDeleted()).isFalse();
        assertThat(student.isEnabled()).isTrue();
        assertThat(student.isLocked()).isFalse();
        assertThat(student.getFailedLoginAttempts()).isZero();
        verify(userRepository).save(student);
    }

    @Test
    @DisplayName("recoverUser : utilisateur déjà actif → IllegalStateException")
    void recoverUser_throws_when_already_active() {
        student.setDeleted(false);
        student.setEnabled(true);

        when(userRepository.findByIdIncludingDeleted(1L)).thenReturn(Optional.of(student));

        assertThatThrownBy(() -> userService.recoverUser(1L))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("recoverUser : id inconnu → RuntimeException")
    void recoverUser_throws_when_user_not_found() {
        when(userRepository.findByIdIncludingDeleted(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.recoverUser(99L))
                .isInstanceOf(RuntimeException.class);
    }

    // ─── getAllUsers / getStats : ADMIN-only ───────────────────────────

    @Test
    @DisplayName("getAllUsers : refuse si non-ADMIN → AccessDeniedException")
    void getAllUsers_denies_non_admin() {
        authenticateAs(student);

        assertThatThrownBy(() -> userService.getAllUsers())
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("getAllUsers : ADMIN → retourne tous les users (incluant deleted)")
    void getAllUsers_returns_all_for_admin() {
        authenticateAs(admin);
        when(userRepository.findAllIncludingDeleted()).thenReturn(List.of(student, admin));
        UserDTO d1 = UserDTO.builder().id(1L).build();
        UserDTO d2 = UserDTO.builder().id(99L).build();
        when(userMapper.toAdminDTO(student)).thenReturn(d1);
        when(userMapper.toAdminDTO(admin)).thenReturn(d2);

        List<UserDTO> result = userService.getAllUsers();
        assertThat(result).containsExactly(d1, d2);
    }

    @Test
    @DisplayName("getStats : refuse si non-ADMIN → AccessDeniedException")
    void getStats_denies_non_admin() {
        authenticateAs(student);

        assertThatThrownBy(() -> userService.getStats())
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("getStats : ADMIN → calcule les agrégats et trend 30j")
    void getStats_returns_aggregates_for_admin() {
        authenticateAs(admin);

        when(userRepository.countAllIncludingDeleted()).thenReturn(10L);
        when(userRepository.countActive()).thenReturn(8L);
        when(userRepository.countDeleted()).thenReturn(2L);
        when(userRepository.countByRoleActive("ADMIN")).thenReturn(1L);
        when(userRepository.countByRoleActive("STUDENT")).thenReturn(7L);
        when(userRepository.countCreatedSince(any())).thenReturn(3L);
        when(userRepository.findAllIncludingDeleted()).thenReturn(List.of(student));

        AdminStatsResponse stats = userService.getStats();

        assertThat(stats.getTotalUsers()).isEqualTo(10);
        assertThat(stats.getActiveUsers()).isEqualTo(8);
        assertThat(stats.getDeletedUsers()).isEqualTo(2);
        assertThat(stats.getAdminCount()).isEqualTo(1);
        assertThat(stats.getStudentCount()).isEqualTo(7);
        assertThat(stats.getNewUsersThisMonth()).isEqualTo(3);
        assertThat(stats.getSignupsByDay()).hasSize(30);
    }
}

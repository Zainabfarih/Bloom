package com.bloom.authservice.controller;

import com.bloom.authservice.dto.AdminStatsResponse;
import com.bloom.authservice.dto.UserDTO;
import com.bloom.authservice.exception.GlobalExceptionHandler;
import com.bloom.authservice.security.JwtAuthFilter;
import com.bloom.authservice.security.JwtService;
import com.bloom.authservice.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = UserController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@ActiveProfiles("test")
class UserControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private UserService userService;

    // Beans transitivement requis par JwtAuthFilter (@Component scanné par @WebMvcTest)
    @MockitoBean private JwtAuthFilter jwtAuthFilter;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private UserDetailsService userDetailsService;

    @Test
    @DisplayName("GET /api/users/{id} — 200")
    void get_user_returns_200() throws Exception {
        UserDTO dto = UserDTO.builder().id(1L).email("alice@bloom.dev").role("STUDENT").build();
        when(userService.getUserById(1L)).thenReturn(dto);

        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("alice@bloom.dev"));
    }

    @Test
    @DisplayName("GET /api/users/{id} — 403 si AccessDeniedException remontée")
    void get_user_returns_403_on_access_denied() throws Exception {
        when(userService.getUserById(2L)).thenThrow(new AccessDeniedException("nope"));

        mockMvc.perform(get("/api/users/2"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    @DisplayName("PUT /api/users/{id} — 200")
    void update_user_returns_200() throws Exception {
        UserDTO updated = UserDTO.builder().id(1L).email("new@bloom.dev").role("STUDENT").build();
        when(userService.updateUser(eq(1L), any())).thenReturn(updated);

        UserDTO req = UserDTO.builder().firstName("New").lastName("Name").email("new@bloom.dev").build();

        mockMvc.perform(put("/api/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("new@bloom.dev"));
    }

    @Test
    @DisplayName("DELETE /api/users/{id} — 204")
    void delete_user_returns_204() throws Exception {
        doNothing().when(userService).softDeleteUser(1L);

        mockMvc.perform(delete("/api/users/1"))
                .andExpect(status().isNoContent());

        verify(userService).softDeleteUser(1L);
    }

    @Test
    @DisplayName("PATCH /api/users/{id}/recover — 204")
    void recover_user_returns_204() throws Exception {
        doNothing().when(userService).recoverUser(1L);

        mockMvc.perform(patch("/api/users/1/recover"))
                .andExpect(status().isNoContent());

        verify(userService).recoverUser(1L);
    }

    @Test
    @DisplayName("GET /api/users — 200 liste admin")
    void list_users_returns_200() throws Exception {
        when(userService.getAllUsers()).thenReturn(List.of(
                UserDTO.builder().id(1L).email("a@bloom.dev").role("STUDENT").build(),
                UserDTO.builder().id(2L).email("b@bloom.dev").role("ADMIN").build()
        ));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("GET /api/users/stats — 200")
    void stats_returns_200() throws Exception {
        AdminStatsResponse stats = AdminStatsResponse.builder()
                .totalUsers(10).activeUsers(8).deletedUsers(2)
                .adminCount(1).studentCount(7).newUsersThisMonth(3)
                .signupsByDay(List.of()).build();
        when(userService.getStats()).thenReturn(stats);

        mockMvc.perform(get("/api/users/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").value(10))
                .andExpect(jsonPath("$.adminCount").value(1));
    }
}

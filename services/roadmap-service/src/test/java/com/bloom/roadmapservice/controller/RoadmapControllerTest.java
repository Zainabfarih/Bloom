package com.bloom.roadmapservice.controller;

import com.bloom.roadmapservice.dto.RoadmapGenerationRequest;
import com.bloom.roadmapservice.dto.RoadmapResponse;
import com.bloom.roadmapservice.dto.StepStatusUpdateDTO;
import com.bloom.roadmapservice.entity.StepStatus;
import com.bloom.roadmapservice.exception.GlobalExceptionHandler;
import com.bloom.roadmapservice.exception.RoadmapNotFoundException;
import com.bloom.roadmapservice.exception.StepNotFoundException;
import com.bloom.roadmapservice.security.GatewayAuthFilter;
import com.bloom.roadmapservice.service.RoadmapService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * On désactive la chaîne de filtres Spring Security et on injecte directement
 * un Principal dans la MockHttpServletRequest. Le résolveur d'argument
 * {@code Authentication} de Spring Web lit alors {@code request.getUserPrincipal()}.
 */
@WebMvcTest(controllers = RoadmapController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@ActiveProfiles("test")
class RoadmapControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private RoadmapService roadmapService;

    // GatewayAuthFilter @Component scanné mais inerte (filters désactivés)
    @MockitoBean private GatewayAuthFilter gatewayAuthFilter;

    /** Authentication avec userId Long en principal — comme produit par GatewayAuthFilter. */
    private static Authentication asUser(Long userId) {
        return new UsernamePasswordAuthenticationToken(
                userId, null, List.of(new SimpleGrantedAuthority("ROLE_STUDENT")));
    }

    private RoadmapResponse sampleResponse() {
        return new RoadmapResponse(10L, 1001L, "Frontend Developer", 50, List.of());
    }

    @Test
    @DisplayName("POST /api/roadmap/generate — 201")
    void generate_returns_201() throws Exception {
        when(roadmapService.generateRoadmap(eq(1L), any())).thenReturn(sampleResponse());

        RoadmapGenerationRequest req = new RoadmapGenerationRequest(1001L);

        mockMvc.perform(post("/api/roadmap/generate")
                        .principal(asUser(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.targetJobTitle").value("Frontend Developer"));
    }

    @Test
    @DisplayName("POST /api/roadmap/generate — 400 si targetJobId null")
    void generate_returns_400_when_target_job_id_missing() throws Exception {
        mockMvc.perform(post("/api/roadmap/generate")
                        .principal(asUser(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.targetJobId").exists());
    }

    @Test
    @DisplayName("GET /api/roadmap — 200 liste pour l'user authentifié")
    void list_returns_200() throws Exception {
        when(roadmapService.getUserRoadmaps(1L)).thenReturn(List.of(sampleResponse()));

        mockMvc.perform(get("/api/roadmap").principal(asUser(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(10));
    }

    @Test
    @DisplayName("PATCH /api/roadmap/steps/{stepId}/status — 200 + nouveau progress")
    void update_step_status_returns_200() throws Exception {
        when(roadmapService.updateStepStatus(eq(1L), eq(99L), any()))
                .thenReturn(new RoadmapResponse(10L, 1001L, "Frontend Developer", 100, List.of()));

        StepStatusUpdateDTO dto = new StepStatusUpdateDTO(StepStatus.COMPLETED);

        mockMvc.perform(patch("/api/roadmap/steps/99/status")
                        .principal(asUser(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.progressPercentage").value(100));
    }

    @Test
    @DisplayName("PATCH /api/roadmap/steps/{stepId}/status — 404 si step inconnue")
    void update_step_status_returns_404_when_step_unknown() throws Exception {
        when(roadmapService.updateStepStatus(eq(1L), eq(99L), any()))
                .thenThrow(new StepNotFoundException(99L));

        StepStatusUpdateDTO dto = new StepStatusUpdateDTO(StepStatus.COMPLETED);

        mockMvc.perform(patch("/api/roadmap/steps/99/status")
                        .principal(asUser(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("GET /api/roadmap/{id} — 200")
    void get_by_id_returns_200() throws Exception {
        when(roadmapService.getRoadmapById(1L, 10L)).thenReturn(sampleResponse());

        mockMvc.perform(get("/api/roadmap/10").principal(asUser(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    @DisplayName("GET /api/roadmap/{id} — 404 si roadmap inconnue")
    void get_by_id_returns_404_when_unknown() throws Exception {
        when(roadmapService.getRoadmapById(1L, 99L))
                .thenThrow(new RoadmapNotFoundException(1L, 99L));

        mockMvc.perform(get("/api/roadmap/99").principal(asUser(1L)))
                .andExpect(status().isNotFound());
    }
}

package com.bloom.jobservice.controller;

import com.bloom.jobservice.dto.*;
import com.bloom.jobservice.service.JobService;
import com.bloom.jobservice.service.SavedJobService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * JobControllerTest — tests du contrôleur avec MockMvc.
 *
 * ══════════════════════════════════════════════════════════════════════
 * COMMENT TESTER SANS USER-SERVICE (en intégration ou manuellement)
 * ══════════════════════════════════════════════════════════════════════
 *
 * Le job-service utilise GatewayAuthFilter qui lit X-User-Id / X-User-Roles.
 * Pour tester les endpoints protégés SANS passer par la gateway ni déployer user-service :
 *
 * Option 1 — Appel direct sur le port 8083 (bypass gateway) :
 *   curl http://localhost:8083/api/job/saved \
 *        -H "X-User-Id: 1" \
 *        -H "X-User-Roles: STUDENT" \
 *        -H "Authorization: Bearer fake-token-ignored-by-job-service"
 *
 * Option 2 — Swagger UI (http://localhost:8083/swagger-ui/index.html) :
 *   Ajoutez un header global dans la config Swagger ou utilisez l'onglet "Authorize".
 *
 * Option 3 — Tests unitaires avec @WithMockUser (utilisé ici) :
 *   @WithMockUser remplace GatewayAuthFilter dans le contexte de test.
 *   Pour tester le principal (userId), utilisez la méthode withHeaders() ci-dessous.
 *
 * Option 4 — Profile "dev" avec SecurityConfig.permitAll() temporaire.
 *   Ajoutez dans application-dev.yaml une propriété app.security.disabled=true
 *   et un @ConditionalOnProperty dans SecurityConfig.
 * ══════════════════════════════════════════════════════════════════════
 */
@WebMvcTest(JobController.class)
class JobControllerTest {

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;

    @MockitoBean private JobService jobService;
    @MockitoBean private SavedJobService savedJobService;

    @Autowired
    public JobControllerTest(MockMvc mockMvc, ObjectMapper objectMapper) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
    }

    // ─── /search ─────────────────────────────────────────────────────────────

    @Test
    void search_returns_200_without_authentication() throws Exception {
        // /search est public — pas besoin de @WithMockUser
        var response = JobSearchResponse.builder()
                .jobs(List.of(
                        JobSearchResult.builder()
                                .jobId("ext-abc")
                                .title("Java Developer")
                                .companyName("ALTEN")
                                .location("Rabat, Morocco")
                                .extensions(List.of("Full-time", "6 days ago"))
                                .build()
                ))
                .fromCache(false)
                .totalResults(1)
                .build();
        when(jobService.searchJobs(eq("java"), eq("Morocco"))).thenReturn(response);

        mockMvc.perform(get("/api/job/search")
                        .param("query", "java")
                        .param("location", "Morocco"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalResults").value(1))
                .andExpect(jsonPath("$.jobs[0].jobId").value("ext-abc"))
                .andExpect(jsonPath("$.jobs[0].title").value("Java Developer"))
                .andExpect(jsonPath("$.jobs[0].companyName").value("ALTEN"))
                // IMPORTANT : pas de description ni extractedSkills dans /search
                .andExpect(jsonPath("$.jobs[0].description").doesNotExist())
                .andExpect(jsonPath("$.jobs[0].extractedSkills").doesNotExist())
                .andExpect(jsonPath("$.fromCache").value(false));
    }

    @Test
    void search_returns_200_from_cache() throws Exception {
        var response = JobSearchResponse.builder()
                .jobs(List.of())
                .fromCache(true)
                .totalResults(0)
                .build();
        when(jobService.searchJobs(eq("python"), isNull())).thenReturn(response);

        mockMvc.perform(get("/api/job/search").param("query", "python"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fromCache").value(true))
                .andExpect(jsonPath("$.totalResults").value(0));
    }

    // ─── /detail/{jobId} ─────────────────────────────────────────────────────

    @Test
    void getJobDetail_returns_200_without_authentication() throws Exception {
        // /api/job/{jobId} est public
        var detail = JobDetailResponse.builder()
                .jobId("ext-abc")
                .title("Java Developer")
                .companyName("ALTEN")
                .location("Rabat, Morocco")
                .description("Design microservices with Spring Boot and Kafka...")
                .extractedSkills(List.of("Java", "Spring Boot", "Kafka", "Docker"))
                .fromSkillCache(false)
                .fromSearchCache(true)
                .build();
        when(jobService.getJobDetail(eq("ext-abc"))).thenReturn(detail);

        mockMvc.perform(get("/api/job/ext-abc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId").value("ext-abc"))
                .andExpect(jsonPath("$.title").value("Java Developer"))
                .andExpect(jsonPath("$.description").exists())
                // Skills présents dans /detail
                .andExpect(jsonPath("$.extractedSkills").isArray())
                .andExpect(jsonPath("$.extractedSkills[0]").value("Docker"))
                .andExpect(jsonPath("$.fromSkillCache").value(false))
                .andExpect(jsonPath("$.fromSearchCache").value(true));
    }

    @Test
    void getJobDetail_returns_200_from_skill_cache() throws Exception {
        var detail = JobDetailResponse.builder()
                .jobId("ext-xyz")
                .title("Senior Java Engineer")
                .extractedSkills(List.of("Java", "Kafka", "PostgreSQL"))
                .fromSkillCache(true)  // vient du cache Redis
                .fromSearchCache(true)
                .build();
        when(jobService.getJobDetail(eq("ext-xyz"))).thenReturn(detail);

        mockMvc.perform(get("/api/job/ext-xyz"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fromSkillCache").value(true))
                .andExpect(jsonPath("$.extractedSkills").isArray());
    }

    // ─── /saved ──────────────────────────────────────────────────────────────

    @Test
    void save_job_returns_401_without_auth_headers() throws Exception {
        // Pas de X-User-Id → 401
        SaveJobRequest request = buildSaveRequest();
        mockMvc.perform(post("/api/job/saved")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "1", roles = "STUDENT")
    void save_job_returns_201_when_valid() throws Exception {
        SaveJobRequest request = buildSaveRequest();
        var response = SavedJobResponse.builder()
                .uuid(UUID.randomUUID())
                .jobTitle("Java Developer")
                .jobExternalId("ext-abc")
                .requiredSkills(List.of("Java", "Spring Boot", "Kafka"))
                .matchedSkills(List.of("Java", "Spring Boot"))
                .missingSkills(List.of("Kafka"))
                .compatibilityScore(66)
                .build();

        when(savedJobService.saveJob(any(), any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/job/saved")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.jobExternalId").value("ext-abc"))
                .andExpect(jsonPath("$.compatibilityScore").value(66))
                .andExpect(jsonPath("$.matchedSkills").isArray())
                .andExpect(jsonPath("$.missingSkills").isArray());
    }

    @Test
    @WithMockUser(username = "1", roles = "STUDENT")
    void save_job_returns_400_when_jobExternalId_blank() throws Exception {
        SaveJobRequest badReq = new SaveJobRequest();
        badReq.setJobTitle("Java Developer"); // jobExternalId manquant

        mockMvc.perform(post("/api/job/saved")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badReq))
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "1", roles = "STUDENT")
    void get_saved_jobs_returns_200() throws Exception {
        when(savedJobService.getSavedJobs(any())).thenReturn(List.of());
        mockMvc.perform(get("/api/job/saved"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @WithMockUser(username = "1", roles = "STUDENT")
    void delete_saved_job_returns_204() throws Exception {
        doNothing().when(savedJobService).removeSavedJob(any(), anyString());
        mockMvc.perform(delete("/api/job/saved/ext-abc").with(csrf()))
                .andExpect(status().isNoContent());
    }

    // ─── Admin ───────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void admin_evict_cache_returns_204_for_admin() throws Exception {
        doNothing().when(jobService).evictCache(anyString(), any());
        mockMvc.perform(delete("/api/job/admin/cache")
                        .param("query", "java")
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void admin_evict_cache_returns_403_for_student() throws Exception {
        mockMvc.perform(delete("/api/job/admin/cache")
                        .param("query", "java")
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private SaveJobRequest buildSaveRequest() {
        SaveJobRequest req = new SaveJobRequest();
        req.setJobExternalId("ext-abc");
        req.setJobTitle("Java Developer");
        req.setJobCompany("ALTEN");
        req.setJobLocation("Rabat, Morocco");
        req.setRequiredSkills(List.of("Java", "Spring Boot", "Kafka"));
        return req;
    }
}

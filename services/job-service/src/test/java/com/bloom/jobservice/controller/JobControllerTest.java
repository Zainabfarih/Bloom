package com.bloom.jobservice.controller;

import com.bloom.jobservice.config.SecurityConfig;
import com.bloom.jobservice.dto.*;
import com.bloom.jobservice.exception.GlobalExceptionHandler;
import com.bloom.jobservice.exception.ResourceNotFoundException;
import com.bloom.jobservice.security.GatewayAuthFilter;
import com.bloom.jobservice.service.JobService;
import com.bloom.jobservice.service.SavedJobService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(JobController.class)
@TestPropertySource(properties = {"internal.security.gateway-secret=test-gateway-secret"})
@Import({SecurityConfig.class, GatewayAuthFilter.class, GlobalExceptionHandler.class})
class JobControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private JobService jobService;

    @MockitoBean
    private SavedJobService savedJobService;


    @Nested
    @DisplayName("GET /api/job/search")
    class SearchTests {

        @Test
        @DisplayName("200 — succès avec résultats depuis SerpAPI")
        void search_returns_200_with_results() throws Exception {

            JobSearchResponse response = JobSearchResponse.builder()
                    .jobs(List.of(buildSearchResult("id-1", "Java Dev")))
                    .fromCache(false)
                    .totalResults(1)
                    .build();

            when(jobService.searchJobs("java", "Morocco")).thenReturn(response);

            mockMvc.perform(get("/api/job/search")
                            .param("query", "java")
                            .param("location", "Morocco"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalResults").value(1))
                    .andExpect(jsonPath("$.fromCache").value(false))
                    .andExpect(jsonPath("$.jobs[0].jobId").value("id-1"));
        }

        @Test
        @DisplayName("200 — succès avec résultats depuis cache Redis")
        void search_returns_200_from_cache() throws Exception {

            JobSearchResponse response = JobSearchResponse.builder()
                    .jobs(List.of())
                    .fromCache(true)
                    .totalResults(0)
                    .build();

            when(jobService.searchJobs("python", null)).thenReturn(response);

            mockMvc.perform(get("/api/job/search").param("query", "python"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.fromCache").value(true));
        }

        @Test
        @DisplayName("400 — erreur si le paramètre query est manquant")
        void search_returns_400_when_query_missing() throws Exception {

            mockMvc.perform(get("/api/job/search"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("503 — erreur si SerpAPI est indisponible")
        void search_returns_503_when_api_fails() throws Exception {

            when(jobService.searchJobs(any(), any()))
                    .thenThrow(new com.bloom.jobservice.exception.JobsApiException("quota exceeded"));

            mockMvc.perform(get("/api/job/search").param("query", "java"))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.code").value("SERVICE_UNAVAILABLE"));
        }
    }


    @Nested
    @DisplayName("GET /api/job/{jobId}")
    class GetDetailTests {

        @Test
        @DisplayName("200 — succès avec détail et compétences")
        void get_detail_returns_200_with_extracted_skills() throws Exception {

            JobDetailResponse detail = JobDetailResponse.builder()
                    .jobId("id-1")
                    .title("Java Developer")
                    .companyName("ALTEN")
                    .description("Spring Boot microservices...")
                    .extractedSkills(List.of("Spring Boot", "Docker", "PostgreSQL"))
                    .fromSkillCache(false)
                    .fromSearchCache(true)
                    .build();

            when(jobService.getJobDetail("id-1")).thenReturn(detail);

            mockMvc.perform(get("/api/job/id-1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.jobId").value("id-1"))
                    .andExpect(jsonPath("$.extractedSkills").isArray())
                    .andExpect(jsonPath("$.extractedSkills[0]").value("Spring Boot"));
        }

        @Test
        @DisplayName("404 — erreur si le job est introuvable")
        void get_detail_returns_404_when_not_cached() throws Exception {

            when(jobService.getJobDetail("unknown"))
                    .thenThrow(new ResourceNotFoundException("Job introuvable en cache"));

            mockMvc.perform(get("/api/job/unknown"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("NOT_FOUND"));
        }
    }


    @Nested
    @DisplayName("POST /api/job/saved")
    class SaveJobTests {

        @Test
        @DisplayName("201 — succès de la sauvegarde avec score")
        void save_job_returns_201_when_authenticated() throws Exception {

            SaveJobRequest request = buildSaveRequest();

            SavedJobResponse response = SavedJobResponse.builder()
                    .uuid(UUID.randomUUID())
                    .jobExternalId("ext-123")
                    .jobTitle("Java Developer")
                    .compatibilityScore(75)
                    .matchedSkills(List.of("Java", "Docker"))
                    .missingSkills(List.of("Kubernetes"))
                    .build();

            when(savedJobService.saveJob(eq(1L), any(), anyString())).thenReturn(response);

            mockMvc.perform(post("/api/job/saved")
                            .header("X-User-Id", "1")
                            .header("X-User-Roles", "STUDENT")
                            .header("Authorization", "Bearer test-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.compatibilityScore").value(75))
                    .andExpect(jsonPath("$.matchedSkills").isArray());
        }

        @Test
        @DisplayName("401 — erreur sans header d'authentification")
        void save_job_returns_401_when_not_authenticated() throws Exception {

            mockMvc.perform(post("/api/job/saved")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildSaveRequest())))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("400 — erreur si le titre est manquant")
        void save_job_returns_400_when_title_missing() throws Exception {

            SaveJobRequest invalid = new SaveJobRequest();
            invalid.setJobExternalId("ext-123");

            mockMvc.perform(post("/api/job/saved")
                            .header("X-User-Id", "1")
                            .header("X-User-Roles", "STUDENT")
                            .header("Authorization", "Bearer test-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalid)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("400 — erreur si l'ID externe est manquant")
        void save_job_returns_400_when_external_id_missing() throws Exception {

            SaveJobRequest invalid = new SaveJobRequest();
            invalid.setJobTitle("Java Dev");

            mockMvc.perform(post("/api/job/saved")
                            .header("X-User-Id", "1")
                            .header("X-User-Roles", "STUDENT")
                            .header("Authorization", "Bearer test-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalid)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("503 — erreur si le cv-service est indisponible")
        void save_job_returns_503_when_cv_service_down() throws Exception {

            when(savedJobService.saveJob(anyLong(), any(), anyString()))
                    .thenThrow(new com.bloom.jobservice.exception.JobsApiException("cv-service indisponible"));

            mockMvc.perform(post("/api/job/saved")
                            .header("X-User-Id", "1")
                            .header("X-User-Roles", "STUDENT")
                            .header("Authorization", "Bearer test-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildSaveRequest())))
                    .andExpect(status().isServiceUnavailable());
        }
    }


    @Nested
    @DisplayName("GET /api/job/saved")
    class GetSavedTests {

        @Test
        @DisplayName("200 — succès avec liste des favoris")
        void get_saved_returns_200_with_list() throws Exception {

            List jobs = List.of(
                    SavedJobResponse.builder()
                            .uuid(UUID.randomUUID())
                            .jobExternalId("ext-1")
                            .compatibilityScore(90)
                            .build(),

                    SavedJobResponse.builder()
                            .uuid(UUID.randomUUID())
                            .jobExternalId("ext-2")
                            .compatibilityScore(60)
                            .build()
            );

            when(savedJobService.getSavedJobs(1L)).thenReturn(jobs);

            mockMvc.perform(get("/api/job/saved")
                            .header("X-User-Id", "1")
                            .header("X-User-Roles", "STUDENT"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2));
        }

        @Test
        @DisplayName("401 — erreur sans header d'authentification")
        void get_saved_returns_401_unauthenticated() throws Exception {

            mockMvc.perform(get("/api/job/saved"))
                    .andExpect(status().isUnauthorized());
        }
    }


    @Nested
    @DisplayName("DELETE /api/job/saved/{jobExternalId}")
    class RemoveSavedTests {

        @Test
        @DisplayName("204 — succès de la suppression")
        void remove_saved_returns_204() throws Exception {

            doNothing().when(savedJobService).removeSavedJob(1L, "ext-123");

            mockMvc.perform(delete("/api/job/saved/ext-123")
                            .header("X-User-Id", "1")
                            .header("X-User-Roles", "STUDENT"))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("404 — erreur si le job n'est pas trouvé")
        void remove_saved_returns_404_when_not_found() throws Exception {

            doThrow(new ResourceNotFoundException("Saved job not found"))
                    .when(savedJobService)
                    .removeSavedJob(1L, "ext-not-exist");

            mockMvc.perform(delete("/api/job/saved/ext-not-exist")
                            .header("X-User-Id", "1")
                            .header("X-User-Roles", "STUDENT"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /api/job/admin/cache")
    class AdminCacheTests {

        @Test
        @DisplayName("204 — succès si appelé avec le rôle ADMIN")
        void admin_evict_cache_returns_204_for_admin() throws Exception {

            doNothing().when(jobService).evictCache("java", "Morocco");

            mockMvc.perform(delete("/api/job/admin/cache")
                            .header("X-User-Id", "99")
                            .header("X-User-Roles", "ADMIN")
                            .param("query", "java")
                            .param("location", "Morocco"))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("403 — refus d'accès pour le rôle STUDENT")
        void admin_evict_cache_returns_403_for_student() throws Exception {

            mockMvc.perform(delete("/api/job/admin/cache")
                            .header("X-User-Id", "1")
                            .header("X-User-Roles", "STUDENT")
                            .param("query", "java"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("401 — erreur sans authentification")
        void admin_evict_cache_returns_401_unauthenticated() throws Exception {

            mockMvc.perform(delete("/api/job/admin/cache")
                            .param("query", "java"))
                    .andExpect(status().isUnauthorized());
        }
    }


    private JobSearchResult buildSearchResult(String id, String title) {

        return JobSearchResult.builder()
                .jobId(id)
                .title(title)
                .companyName("ALTEN")
                .location("Morocco")
                .extensions(List.of("Full-time"))
                .build();
    }

    private SaveJobRequest buildSaveRequest() {

        SaveJobRequest req = new SaveJobRequest();
        req.setJobExternalId("ext-123");
        req.setJobTitle("Java Developer");
        req.setJobCompany("TechCorp");
        req.setJobLocation("Casablanca");
        req.setRequiredSkills(List.of("Java", "Docker", "Kubernetes"));
        return req;
    }
}
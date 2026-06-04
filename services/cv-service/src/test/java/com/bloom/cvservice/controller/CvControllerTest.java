package com.bloom.cvservice.controller;

import com.bloom.cvservice.config.SecurityConfig;
import com.bloom.cvservice.dto.CvAnalysisResponse;
import com.bloom.cvservice.dto.CvResponse;
import com.bloom.cvservice.dto.ManualCvRequest;
import com.bloom.cvservice.dto.SkillsDTO;
import com.bloom.cvservice.entity.CvSource;
import com.bloom.cvservice.exception.CvProcessingException;
import com.bloom.cvservice.exception.GlobalExceptionHandler;
import com.bloom.cvservice.exception.ResourceNotFoundException;
import com.bloom.cvservice.security.GatewayAuthFilter;
import com.bloom.cvservice.service.CvService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CvController.class)
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "internal.security.gateway-secret=test-gateway-secret",
        "jwt.secret=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970",
        "spring.cloud.config.enabled=false",
        "spring.config.import=",
        "eureka.client.enabled=false",
        "eureka.client.register-with-eureka=false",
        "eureka.client.fetch-registry=false",
        "cv.ai.gemini.key=test-gemini-key",
        "cv.ai.hf.skill-extractor-url=http://localhost/hf-mock"
})
@Import({SecurityConfig.class, GatewayAuthFilter.class, GlobalExceptionHandler.class})
class CvControllerTest {

    private static final String GATEWAY_SECRET = "test-gateway-secret";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CvService cvService;


    @Nested
    @DisplayName("POST /api/cv/upload")
    class UploadTests {

        @Test
        @DisplayName("201 — upload PDF réussi avec skills extraits")
        void upload_returns_201() throws Exception {

            CvResponse response = CvResponse.builder()
                    .uuid(UUID.randomUUID())
                    .title("mon-cv.pdf")
                    .source(CvSource.UPLOAD)
                    .skills(List.of("Java", "Spring Boot"))
                    .active(true)
                    .build();

            when(cvService.uploadCv(eq(1L), any(), any())).thenReturn(response);

            MockMultipartFile file = new MockMultipartFile(
                    "file", "mon-cv.pdf", MediaType.APPLICATION_PDF_VALUE, "%PDF-1.4 fake".getBytes());

            mockMvc.perform(multipart("/api/cv/upload")
                            .file(file)
                            .header("X-Gateway-Secret", GATEWAY_SECRET)
                            .header("X-User-Id", "1")
                            .header("X-User-Role", "STUDENT"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.source").value("UPLOAD"))
                    .andExpect(jsonPath("$.skills[0]").value("Java"));
        }

        @Test
        @DisplayName("401 — refus sans authentification")
        void upload_returns_401_unauthenticated() throws Exception {

            MockMultipartFile file = new MockMultipartFile(
                    "file", "cv.pdf", MediaType.APPLICATION_PDF_VALUE, "data".getBytes());

            mockMvc.perform(multipart("/api/cv/upload").file(file))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("422 — erreur si le fichier n'est pas un PDF valide")
        void upload_returns_422_when_not_pdf() throws Exception {

            when(cvService.uploadCv(anyLong(), any(), any()))
                    .thenThrow(new CvProcessingException("Seuls les fichiers PDF sont acceptés."));

            MockMultipartFile file = new MockMultipartFile(
                    "file", "cv.txt", MediaType.TEXT_PLAIN_VALUE, "hello".getBytes());

            mockMvc.perform(multipart("/api/cv/upload")
                            .file(file)
                            .header("X-Gateway-Secret", GATEWAY_SECRET)
                            .header("X-User-Id", "1")
                            .header("X-User-Role", "STUDENT"))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.code").value("CV_PROCESSING_ERROR"));
        }
    }


    @Nested
    @DisplayName("POST /api/cv/manual")
    class ManualTests {

        @Test
        @DisplayName("201 — création manuelle réussie")
        void manual_returns_201() throws Exception {

            CvResponse response = CvResponse.builder()
                    .uuid(UUID.randomUUID())
                    .source(CvSource.MANUAL)
                    .skills(List.of("Python"))
                    .active(true)
                    .build();

            when(cvService.createManualCv(eq(1L), any())).thenReturn(response);

            ManualCvRequest request = new ManualCvRequest();
            request.setTitle("Mon CV");
            request.setSummary("Étudiant en génie logiciel passionné par le backend.");
            request.setExperiences(List.of("Stage backend 6 mois chez X"));
            request.setEducations(List.of("Master Génie Logiciel - 2026"));
            request.setSkills(List.of("Python"));

            mockMvc.perform(post("/api/cv/manual")
                            .header("X-Gateway-Secret", GATEWAY_SECRET)
                            .header("X-User-Id", "1")
                            .header("X-User-Role", "STUDENT")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.source").value("MANUAL"));
        }

        @Test
        @DisplayName("400 — erreur si le résumé est manquant")
        void manual_returns_400_when_summary_missing() throws Exception {

            ManualCvRequest invalid = new ManualCvRequest();
            invalid.setExperiences(List.of("Stage backend"));
            invalid.setEducations(List.of("Master GL"));
            invalid.setSkills(List.of("Python"));

            mockMvc.perform(post("/api/cv/manual")
                            .header("X-Gateway-Secret", GATEWAY_SECRET)
                            .header("X-User-Id", "1")
                            .header("X-User-Role", "STUDENT")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalid)))
                    .andExpect(status().isBadRequest());
        }
    }


    @Nested
    @DisplayName("GET /api/cv/users/{userId}/skills (contrat job-service)")
    class UserSkillsTests {

        @Test
        @DisplayName("200 — skills du CV actif")
        void get_user_skills_returns_200() throws Exception {

            SkillsDTO dto = SkillsDTO.builder()
                    .userId(1L)
                    .cvUuid(UUID.randomUUID())
                    .skills(List.of("Java", "Docker"))
                    .build();

            when(cvService.getUserSkills(1L)).thenReturn(dto);

            mockMvc.perform(get("/api/cv/users/1/skills")
                            .header("X-Gateway-Secret", GATEWAY_SECRET)
                            .header("X-User-Id", "1")
                            .header("X-User-Role", "STUDENT"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.userId").value(1))
                    .andExpect(jsonPath("$.skills.length()").value(2));
        }

        @Test
        @DisplayName("404 — aucun CV actif")
        void get_user_skills_returns_404() throws Exception {

            when(cvService.getUserSkills(99L))
                    .thenThrow(new ResourceNotFoundException("Aucun CV actif"));

            mockMvc.perform(get("/api/cv/users/99/skills")
                            .header("X-Gateway-Secret", GATEWAY_SECRET)
                            .header("X-User-Id", "99")
                            .header("X-User-Role", "STUDENT"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("NOT_FOUND"));
        }
    }


    @Nested
    @DisplayName("GET /api/cv/{cvUuid}/analysis")
    class AnalysisTests {

        @Test
        @DisplayName("200 — analyse ATS avec score et issues")
        void analyze_returns_200() throws Exception {

            UUID cvUuid = UUID.randomUUID();
            CvAnalysisResponse analysis = CvAnalysisResponse.builder()
                    .cvUuid(cvUuid)
                    .atsScore(82)
                    .summary("CV solide, quelques améliorations de forme.")
                    .strengths(List.of("Bonne structure"))
                    .issues(List.of())
                    .build();

            when(cvService.analyzeCv(eq(1L), eq(cvUuid))).thenReturn(analysis);

            mockMvc.perform(get("/api/cv/" + cvUuid + "/analysis")
                            .header("X-Gateway-Secret", GATEWAY_SECRET)
                            .header("X-User-Id", "1")
                            .header("X-User-Role", "STUDENT"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.atsScore").value(82))
                    .andExpect(jsonPath("$.cvUuid").value(cvUuid.toString()));
        }
    }


    @Nested
    @DisplayName("DELETE /api/cv/{cvUuid}")
    class DeleteTests {

        @Test
        @DisplayName("204 — suppression réussie")
        void delete_returns_204() throws Exception {

            UUID cvUuid = UUID.randomUUID();
            doNothing().when(cvService).deleteCv(1L, cvUuid);

            mockMvc.perform(delete("/api/cv/" + cvUuid)
                            .header("X-Gateway-Secret", GATEWAY_SECRET)
                            .header("X-User-Id", "1")
                            .header("X-User-Role", "STUDENT"))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("404 — CV introuvable")
        void delete_returns_404() throws Exception {

            UUID cvUuid = UUID.randomUUID();
            doThrow(new ResourceNotFoundException("CV introuvable"))
                    .when(cvService).deleteCv(1L, cvUuid);

            mockMvc.perform(delete("/api/cv/" + cvUuid)
                            .header("X-Gateway-Secret", GATEWAY_SECRET)
                            .header("X-User-Id", "1")
                            .header("X-User-Role", "STUDENT"))
                    .andExpect(status().isNotFound());
        }
    }
}

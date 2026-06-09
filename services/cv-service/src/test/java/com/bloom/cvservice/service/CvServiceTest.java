package com.bloom.cvservice.service;

import com.bloom.cvservice.dto.CvAnalysisResponse;
import com.bloom.cvservice.dto.CvResponse;
import com.bloom.cvservice.dto.ManualCvRequest;
import com.bloom.cvservice.dto.SkillsDTO;
import com.bloom.cvservice.entity.Cv;
import com.bloom.cvservice.entity.CvSource;
import com.bloom.cvservice.exception.CvProcessingException;
import com.bloom.cvservice.exception.ResourceNotFoundException;
import com.bloom.cvservice.mapper.CvMapper;
import com.bloom.cvservice.repository.CvRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CvServiceTest {

    @Mock private CvRepository      cvRepository;
    @Mock private PdfTextExtractor  pdfTextExtractor;
    @Mock private CvSkillExtractor  cvSkillExtractor;
    @Mock private CvAnalysisService cvAnalysisService;
    @Mock private CvMapper          cvMapper;

    @InjectMocks
    private CvService cvService;

    private static final Long USER_ID = 1L;
    private static final UUID CV_UUID = UUID.randomUUID();

    private CvResponse response;

    @BeforeEach
    void setUp() {
        response = CvResponse.builder()
                .uuid(CV_UUID)
                .title("Mon CV")
                .source(CvSource.UPLOAD)
                .skills(List.of("Java"))
                .active(true)
                .build();
    }


    @Nested
    @DisplayName("uploadCv")
    class UploadCvTests {

        @Test
        @DisplayName("Désactive l'ancien CV, extrait les skills et persiste un CV UPLOAD")
        void upload_extracts_skills_and_persists() {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "cv.pdf", "application/pdf", "%PDF-1.4 fake".getBytes());

            when(pdfTextExtractor.extract(any(MultipartFile.class))).thenReturn("texte du cv");
            when(cvSkillExtractor.extract("texte du cv")).thenReturn(List.of("Java", "Spring"));
            when(cvRepository.findByUserIdAndActiveTrue(USER_ID)).thenReturn(Optional.empty());
            when(cvRepository.save(any(Cv.class))).thenAnswer(inv -> inv.getArgument(0));
            when(cvMapper.toResponse(any(Cv.class))).thenReturn(response);

            CvResponse result = cvService.uploadCv(USER_ID, file, "Mon CV");

            assertThat(result).isNotNull();

            ArgumentCaptor<Cv> captor = ArgumentCaptor.forClass(Cv.class);
            verify(cvRepository).save(captor.capture());
            Cv saved = captor.getValue();
            assertThat(saved.getSource()).isEqualTo(CvSource.UPLOAD);
            assertThat(saved.getUserId()).isEqualTo(USER_ID);
            assertThat(saved.getSkillNames()).containsExactly("Java", "Spring");
            assertThat(saved.getFileData()).isNotEmpty();
        }

        @Test
        @DisplayName("Désactive le CV actif existant avant l'upload")
        void upload_deactivates_existing_active_cv() {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "cv.pdf", "application/pdf", "%PDF".getBytes());
            Cv existing = buildCv(UUID.randomUUID(), USER_ID, true, null, List.of());

            when(pdfTextExtractor.extract(any(MultipartFile.class))).thenReturn("t");
            when(cvSkillExtractor.extract("t")).thenReturn(List.of());
            when(cvRepository.findByUserIdAndActiveTrue(USER_ID)).thenReturn(Optional.of(existing));
            when(cvRepository.save(any(Cv.class))).thenAnswer(inv -> inv.getArgument(0));
            when(cvMapper.toResponse(any(Cv.class))).thenReturn(response);

            cvService.uploadCv(USER_ID, file, null);

            assertThat(existing.isActive()).isFalse();
            verify(cvRepository).saveAndFlush(existing);
        }

        @Test
        @DisplayName("Fichier illisible (IOException) → CvProcessingException")
        void upload_throws_when_file_unreadable() throws IOException {
            MultipartFile file = org.mockito.Mockito.mock(MultipartFile.class);
            when(pdfTextExtractor.extract(file)).thenReturn("t");
            when(cvSkillExtractor.extract("t")).thenReturn(List.of());
            when(file.getBytes()).thenThrow(new IOException("io"));

            assertThatThrownBy(() -> cvService.uploadCv(USER_ID, file, null))
                    .isInstanceOf(CvProcessingException.class)
                    .hasMessageContaining("Impossible de lire");

            verify(cvRepository, never()).save(any());
        }
    }


    @Nested
    @DisplayName("createManualCv")
    class CreateManualCvTests {

        @Test
        @DisplayName("Décode le PDF du front, persiste un CV MANUAL avec les skills fournis")
        void manual_decodes_pdf_and_persists() {
            ManualCvRequest req = manualRequest("JVBERi0xLjQK");

            when(cvRepository.findByUserIdAndActiveTrue(USER_ID)).thenReturn(Optional.empty());
            when(cvRepository.save(any(Cv.class))).thenAnswer(inv -> inv.getArgument(0));
            when(cvMapper.toResponse(any(Cv.class))).thenReturn(response);

            cvService.createManualCv(USER_ID, req);

            ArgumentCaptor<Cv> captor = ArgumentCaptor.forClass(Cv.class);
            verify(cvRepository).save(captor.capture());
            Cv saved = captor.getValue();
            assertThat(saved.getSource()).isEqualTo(CvSource.MANUAL);
            assertThat(saved.getFileData()).isNotEmpty();
            assertThat(saved.getSkillNames()).containsExactly("Java");
            assertThat(saved.getDescription()).contains("SUMMARY").contains("SKILLS");
        }

        @Test
        @DisplayName("Préfixe data-URI accepté et retiré avant décodage")
        void manual_strips_data_uri_prefix() {
            ManualCvRequest req = manualRequest("data:application/pdf;base64,JVBERi0xLjQK");

            when(cvRepository.findByUserIdAndActiveTrue(USER_ID)).thenReturn(Optional.empty());
            when(cvRepository.save(any(Cv.class))).thenAnswer(inv -> inv.getArgument(0));
            when(cvMapper.toResponse(any(Cv.class))).thenReturn(response);

            cvService.createManualCv(USER_ID, req);

            ArgumentCaptor<Cv> captor = ArgumentCaptor.forClass(Cv.class);
            verify(cvRepository).save(captor.capture());
            assertThat(captor.getValue().getFileData()).isNotEmpty();
        }

        @Test
        @DisplayName("Base64 invalide → CvProcessingException, aucune persistance")
        void manual_throws_on_invalid_base64() {
            ManualCvRequest req = manualRequest("@@not-base64@@");

            assertThatThrownBy(() -> cvService.createManualCv(USER_ID, req))
                    .isInstanceOf(CvProcessingException.class)
                    .hasMessageContaining("base64 invalide");

            verify(cvRepository, never()).save(any());
        }

        @Test
        @DisplayName("PDF vide après décodage → CvProcessingException")
        void manual_throws_on_empty_pdf() {
            ManualCvRequest req = manualRequest("");

            assertThatThrownBy(() -> cvService.createManualCv(USER_ID, req))
                    .isInstanceOf(CvProcessingException.class)
                    .hasMessageContaining("vide");

            verify(cvRepository, never()).save(any());
        }
    }


    @Nested
    @DisplayName("Lectures")
    class ReadTests {

        @Test
        @DisplayName("getActiveCv → mappe le CV actif")
        void get_active_cv_returns_mapped() {
            Cv cv = buildCv(CV_UUID, USER_ID, true, "desc", List.of("Java"));
            when(cvRepository.findActiveByUserIdWithSkills(USER_ID)).thenReturn(Optional.of(cv));
            when(cvMapper.toResponse(cv)).thenReturn(response);

            assertThat(cvService.getActiveCv(USER_ID)).isEqualTo(response);
        }

        @Test
        @DisplayName("getActiveCv sans CV actif → ResourceNotFoundException")
        void get_active_cv_throws_when_none() {
            when(cvRepository.findActiveByUserIdWithSkills(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cvService.getActiveCv(USER_ID))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("getCvSkills → SkillsDTO du CV demandé")
        void get_cv_skills_returns_dto() {
            Cv cv = buildCv(CV_UUID, USER_ID, true, null, List.of("Java", "Docker"));
            SkillsDTO dto = SkillsDTO.builder().userId(USER_ID).cvUuid(CV_UUID)
                    .skills(List.of("Java", "Docker")).build();
            when(cvRepository.findByUuidWithSkills(CV_UUID)).thenReturn(Optional.of(cv));
            when(cvMapper.toSkillsDTO(cv)).thenReturn(dto);

            assertThat(cvService.getCvSkills(CV_UUID).getSkills()).containsExactly("Java", "Docker");
        }

        @Test
        @DisplayName("getCvSkills UUID inconnu → ResourceNotFoundException")
        void get_cv_skills_throws_when_not_found() {
            when(cvRepository.findByUuidWithSkills(CV_UUID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cvService.getCvSkills(CV_UUID))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("getOwnedCv mauvais propriétaire → ResourceNotFoundException")
        void get_owned_cv_throws_for_wrong_owner() {
            Cv cv = buildCv(CV_UUID, 999L, true, null, List.of());
            when(cvRepository.findByUuid(CV_UUID)).thenReturn(Optional.of(cv));

            assertThatThrownBy(() -> cvService.getOwnedCv(USER_ID, CV_UUID))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }


    @Nested
    @DisplayName("analyzeCv")
    class AnalyzeCvTests {

        @Test
        @DisplayName("CV manuel : utilise la description, n'extrait pas le PDF")
        void analyze_manual_uses_description() {
            Cv cv = buildCv(CV_UUID, USER_ID, true, "SUMMARY\nProfil backend", List.of("Java"));
            CvAnalysisResponse analysis = CvAnalysisResponse.builder().atsScore(80).build();
            when(cvRepository.findByUuidWithSkills(CV_UUID)).thenReturn(Optional.of(cv));
            when(cvAnalysisService.analyze(any())).thenReturn(analysis);

            CvAnalysisResponse result = cvService.analyzeCv(USER_ID, CV_UUID);

            assertThat(result.getCvUuid()).isEqualTo(CV_UUID);
            verify(pdfTextExtractor, never()).extract(any(byte[].class));
        }

        @Test
        @DisplayName("CV uploadé : ré-extrait le texte du PDF stocké")
        void analyze_uploaded_extracts_pdf() {
            Cv cv = buildCv(CV_UUID, USER_ID, true, null, List.of());
            cv.setFileData("%PDF".getBytes());
            CvAnalysisResponse analysis = CvAnalysisResponse.builder().atsScore(55).build();
            when(cvRepository.findByUuidWithSkills(CV_UUID)).thenReturn(Optional.of(cv));
            when(pdfTextExtractor.extract(any(byte[].class))).thenReturn("texte extrait");
            when(cvAnalysisService.analyze(any())).thenReturn(analysis);

            CvAnalysisResponse result = cvService.analyzeCv(USER_ID, CV_UUID);

            assertThat(result.getAtsScore()).isEqualTo(55);
            verify(pdfTextExtractor).extract(any(byte[].class));
        }

        @Test
        @DisplayName("Mauvais propriétaire → ResourceNotFoundException")
        void analyze_throws_for_wrong_owner() {
            Cv cv = buildCv(CV_UUID, 999L, true, "desc", List.of());
            when(cvRepository.findByUuidWithSkills(CV_UUID)).thenReturn(Optional.of(cv));

            assertThatThrownBy(() -> cvService.analyzeCv(USER_ID, CV_UUID))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }


    @Nested
    @DisplayName("deleteCv")
    class DeleteCvTests {

        @Test
        @DisplayName("Supprime un CV actif et promeut le plus récent restant")
        void delete_active_promotes_most_recent() {
            Cv active = buildCv(CV_UUID, USER_ID, true, null, List.of());
            Cv other  = buildCv(UUID.randomUUID(), USER_ID, false, null, List.of());

            when(cvRepository.findByUuid(CV_UUID)).thenReturn(Optional.of(active));
            when(cvRepository.findByUserId(USER_ID)).thenReturn(List.of(other));

            cvService.deleteCv(USER_ID, CV_UUID);

            verify(cvRepository).delete(active);
            assertThat(other.isActive()).isTrue();
            verify(cvRepository).save(other);
        }

        @Test
        @DisplayName("Supprime un CV non actif : pas de promotion")
        void delete_inactive_does_not_promote() {
            Cv inactive = buildCv(CV_UUID, USER_ID, false, null, List.of());

            when(cvRepository.findByUuid(CV_UUID)).thenReturn(Optional.of(inactive));

            cvService.deleteCv(USER_ID, CV_UUID);

            verify(cvRepository).delete(inactive);
            verify(cvRepository, never()).findByUserId(any());
            verify(cvRepository, never()).save(any());
        }

        @Test
        @DisplayName("Mauvais propriétaire → ResourceNotFoundException")
        void delete_throws_for_wrong_owner() {
            Cv cv = buildCv(CV_UUID, 999L, true, null, List.of());
            when(cvRepository.findByUuid(CV_UUID)).thenReturn(Optional.of(cv));

            assertThatThrownBy(() -> cvService.deleteCv(USER_ID, CV_UUID))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(cvRepository, never()).delete(any());
        }
    }


    private ManualCvRequest manualRequest(String pdfBase64) {
        ManualCvRequest req = new ManualCvRequest();
        req.setTitle("Mon CV");
        req.setSummary("Etudiant backend.");
        req.setExperiences(List.of("Stage backend 6 mois"));
        req.setEducations(List.of("Master GL 2026"));
        req.setSkills(List.of("Java"));
        req.setPdfBase64(pdfBase64);
        return req;
    }

    private Cv buildCv(UUID uuid, Long userId, boolean active, String description, List<String> skills) {
        Cv cv = Cv.builder()
                .uuid(uuid)
                .userId(userId)
                .title("CV")
                .source(description != null ? CvSource.MANUAL : CvSource.UPLOAD)
                .active(active)
                .description(description)
                .build();
        cv.replaceSkills(skills);
        return cv;
    }
}

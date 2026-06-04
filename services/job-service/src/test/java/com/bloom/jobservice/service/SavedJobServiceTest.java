package com.bloom.jobservice.service;

import com.bloom.jobservice.dto.JobDetailResponse;
import com.bloom.jobservice.dto.SavedJobResponse;
import com.bloom.jobservice.dto.SkillsDTO;
import com.bloom.jobservice.entity.SavedJob;
import com.bloom.jobservice.exception.JobsApiException;
import com.bloom.jobservice.exception.ResourceNotFoundException;
import com.bloom.jobservice.external.CvServiceClient;
import com.bloom.jobservice.mapper.SavedJobMapper;
import com.bloom.jobservice.repository.SavedJobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SavedJobServiceTest {

    @Mock private SavedJobRepository   savedJobRepository;
    @Mock private SkillMatchingService skillMatchingService;
    @Mock private CvServiceClient      cvServiceClient;
    @Mock private JobService           jobService;
    @Mock private SavedJobMapper       savedJobMapper;

    @InjectMocks
    private SavedJobService savedJobService;

    private static final Long   USER_ID    = 1L;
    private static final String BEARER     = "Bearer test-token";
    private static final String JOB_EXT_ID = "ext-123";
    private static final UUID   CV_UUID    = UUID.randomUUID();

    private JobDetailResponse jobDetail;

    @BeforeEach
    void setUp() {
        jobDetail = JobDetailResponse.builder()
                .jobId(JOB_EXT_ID)
                .title("Java Developer")
                .companyName("TechCorp")
                .location("Casablanca")
                .extractedSkills(List.of("Java", "Docker", "PostgreSQL"))
                .build();
    }


    @Nested
    @DisplayName("saveJob")
    class SaveJobTests {

        @Test
        @DisplayName("Nouveau job : calcule le matching et persiste")
        void save_new_job_computes_matching_and_persists() {
            SkillsDTO cvData = buildCvSkills(CV_UUID, List.of("Java", "Python"));
            SavedJob  entity = buildSavedJob(50);
            SavedJobResponse response = buildResponse(entity);

            when(savedJobRepository.findByUserIdAndJobExternalId(USER_ID, JOB_EXT_ID))
                    .thenReturn(Optional.empty());
            when(jobService.getJobForSave(JOB_EXT_ID)).thenReturn(jobDetail);
            when(cvServiceClient.getUserSkills(USER_ID, BEARER)).thenReturn(cvData);
            when(skillMatchingService.findMatched(any(), any())).thenReturn(List.of("Java"));
            when(skillMatchingService.findMissing(any(), any())).thenReturn(List.of("Docker", "PostgreSQL"));
            when(skillMatchingService.computeScore(any(), any())).thenReturn(50);
            when(savedJobRepository.save(any())).thenReturn(entity);
            when(savedJobMapper.toResponse(entity)).thenReturn(response);

            SavedJobResponse result = savedJobService.saveJob(USER_ID, JOB_EXT_ID, null, BEARER);

            assertThat(result).isNotNull();
            assertThat(result.getCompatibilityScore()).isEqualTo(50);
            verify(savedJobRepository).save(any());
            verify(skillMatchingService).computeScore(any(), any());
            verify(cvServiceClient).getUserSkills(USER_ID, BEARER);
            verify(cvServiceClient, never()).getCvSkills(any(), any());
        }

        @Test
        @DisplayName("Idempotent : retourne l'existant sans doublon ni appel cv-service")
        void save_job_is_idempotent_when_already_saved() {
            SavedJob existing = buildSavedJob(75);
            SavedJobResponse response = buildResponse(existing);

            when(savedJobRepository.findByUserIdAndJobExternalId(USER_ID, JOB_EXT_ID))
                    .thenReturn(Optional.of(existing));
            when(savedJobMapper.toResponse(existing)).thenReturn(response);

            SavedJobResponse result = savedJobService.saveJob(USER_ID, JOB_EXT_ID, null, BEARER);

            assertThat(result.getJobExternalId()).isEqualTo(JOB_EXT_ID);
            assertThat(result.getCompatibilityScore()).isEqualTo(75);
            verify(savedJobRepository, never()).save(any());
            verify(jobService, never()).getJobForSave(anyString());
            verify(cvServiceClient, never()).getUserSkills(anyLong(), anyString());
            verify(cvServiceClient, never()).getCvSkills(any(), anyString());
        }

        @Test
        @DisplayName("cvUuid fourni → appelle getCvSkills (cv spécifique), pas getUserSkills")
        void save_job_with_specific_cv_uuid_calls_getCvSkills() {
            SkillsDTO cvData = buildCvSkills(CV_UUID, List.of("Java", "Spring Boot"));
            SavedJob  entity = buildSavedJob(33);
            SavedJobResponse response = buildResponse(entity);

            when(savedJobRepository.findByUserIdAndJobExternalId(USER_ID, JOB_EXT_ID))
                    .thenReturn(Optional.empty());
            when(jobService.getJobForSave(JOB_EXT_ID)).thenReturn(jobDetail);
            when(cvServiceClient.getCvSkills(CV_UUID, BEARER)).thenReturn(cvData);
            when(skillMatchingService.findMatched(any(), any())).thenReturn(List.of("Java", "Spring Boot"));
            when(skillMatchingService.findMissing(any(), any())).thenReturn(List.of("Docker"));
            when(skillMatchingService.computeScore(any(), any())).thenReturn(33);
            when(savedJobRepository.save(any())).thenReturn(entity);
            when(savedJobMapper.toResponse(entity)).thenReturn(response);

            savedJobService.saveJob(USER_ID, JOB_EXT_ID, CV_UUID, BEARER);

            verify(cvServiceClient).getCvSkills(CV_UUID, BEARER);
            verify(cvServiceClient, never()).getUserSkills(anyLong(), anyString());
        }

        @Test
        @DisplayName("cv-service KO → lève JobsApiException (pas de sauvegarde partielle)")
        void save_job_throws_JobsApiException_when_cv_service_is_down() {
            when(savedJobRepository.findByUserIdAndJobExternalId(USER_ID, JOB_EXT_ID))
                    .thenReturn(Optional.empty());
            when(jobService.getJobForSave(JOB_EXT_ID)).thenReturn(jobDetail);
            when(cvServiceClient.getUserSkills(USER_ID, BEARER))
                    .thenThrow(new RuntimeException("Connection refused"));

            assertThatThrownBy(() -> savedJobService.saveJob(USER_ID, JOB_EXT_ID, null, BEARER))
                    .isInstanceOf(JobsApiException.class)
                    .hasMessageContaining("cv-service indisponible");

            verify(savedJobRepository, never()).save(any());
        }

        @Test
        @DisplayName("Job absent du cache → lève ResourceNotFoundException")
        void save_job_throws_when_job_not_in_cache() {
            when(savedJobRepository.findByUserIdAndJobExternalId(USER_ID, JOB_EXT_ID))
                    .thenReturn(Optional.empty());
            when(jobService.getJobForSave(JOB_EXT_ID))
                    .thenThrow(new ResourceNotFoundException("Job not found in cache"));

            assertThatThrownBy(() -> savedJobService.saveJob(USER_ID, JOB_EXT_ID, null, BEARER))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(savedJobRepository, never()).save(any());
        }
    }


    @Nested
    @DisplayName("getSavedJobs")
    class GetSavedJobsTests {

        @Test
        @DisplayName("Retourne la liste mappée depuis le repository")
        void get_saved_jobs_returns_mapped_list() {
            SavedJob job1 = buildSavedJob(100);
            SavedJob job2 = buildSavedJob(60);
            when(savedJobRepository.findByUserIdWithSkills(USER_ID))
                    .thenReturn(List.of(job1, job2));
            when(savedJobMapper.toResponse(job1)).thenReturn(buildResponse(job1));
            when(savedJobMapper.toResponse(job2)).thenReturn(buildResponse(job2));

            List<SavedJobResponse> results = savedJobService.getSavedJobs(USER_ID);

            assertThat(results).hasSize(2);
            verify(savedJobRepository).findByUserIdWithSkills(USER_ID);
        }

        @Test
        @DisplayName("Retourne une liste vide si aucun job sauvegardé")
        void get_saved_jobs_returns_empty_list_when_none() {
            when(savedJobRepository.findByUserIdWithSkills(USER_ID)).thenReturn(List.of());

            List<SavedJobResponse> results = savedJobService.getSavedJobs(USER_ID);

            assertThat(results).isEmpty();
        }
    }


    @Nested
    @DisplayName("getByUuid")
    class GetByUuidTests {

        @Test
        @DisplayName("Propriétaire légitime → retourne le job avec les bons champs")
        void get_by_uuid_returns_job_for_owner() {
            UUID uuid = UUID.randomUUID();
            SavedJob job = buildSavedJob(80);
            job.setUserId(USER_ID);
            SavedJobResponse response = buildResponse(job);

            when(savedJobRepository.findByUuidWithSkills(uuid)).thenReturn(Optional.of(job));
            when(savedJobMapper.toResponse(job)).thenReturn(response);

            SavedJobResponse result = savedJobService.getByUuid(USER_ID, uuid);

            assertThat(result.getCompatibilityScore()).isEqualTo(80);
            assertThat(result.getJobExternalId()).isEqualTo(JOB_EXT_ID);
        }

        @Test
        @DisplayName("Mauvais propriétaire → lève ResourceNotFoundException (ownership check)")
        void get_by_uuid_throws_when_wrong_user() {
            UUID uuid = UUID.randomUUID();
            SavedJob job = buildSavedJob(80);
            job.setUserId(999L);

            when(savedJobRepository.findByUuidWithSkills(uuid)).thenReturn(Optional.of(job));

            assertThatThrownBy(() -> savedJobService.getByUuid(USER_ID, uuid))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("UUID inexistant → lève ResourceNotFoundException")
        void get_by_uuid_throws_when_not_found() {
            UUID uuid = UUID.randomUUID();
            when(savedJobRepository.findByUuidWithSkills(uuid)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> savedJobService.getByUuid(USER_ID, uuid))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining(uuid.toString());
        }
    }


    @Nested
    @DisplayName("removeSavedJob")
    class RemoveSavedJobTests {

        @Test
        @DisplayName("Suppression réussie si le job existe")
        void remove_saved_job_succeeds_when_found() {
            when(savedJobRepository.deleteByUserIdAndJobExternalId(USER_ID, JOB_EXT_ID))
                    .thenReturn(1);

            savedJobService.removeSavedJob(USER_ID, JOB_EXT_ID);

            verify(savedJobRepository).deleteByUserIdAndJobExternalId(USER_ID, JOB_EXT_ID);
        }

        @Test
        @DisplayName("Lève ResourceNotFoundException si 0 ligne supprimée")
        void remove_saved_job_throws_when_not_found() {
            when(savedJobRepository.deleteByUserIdAndJobExternalId(USER_ID, JOB_EXT_ID))
                    .thenReturn(0);

            assertThatThrownBy(() -> savedJobService.removeSavedJob(USER_ID, JOB_EXT_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining(String.valueOf(USER_ID));
        }
    }


    private SkillsDTO buildCvSkills(UUID cvUuid, List<String> skills) {
        SkillsDTO dto = new SkillsDTO();
        dto.setUserId(USER_ID);
        dto.setCvUuid(cvUuid);
        dto.setSkills(skills);
        return dto;
    }

    private SavedJob buildSavedJob(int score) {
        return SavedJob.builder()
                .id(1L)
                .uuid(UUID.randomUUID())
                .userId(USER_ID)
                .cvUuid(CV_UUID)
                .jobExternalId(JOB_EXT_ID)
                .jobTitle("Java Developer")
                .jobCompany("TechCorp")
                .compatibilityScore(score)
                .savedAt(Instant.now())
                .build();
    }

    private SavedJobResponse buildResponse(SavedJob job) {
        return SavedJobResponse.builder()
                .uuid(job.getUuid())
                .jobExternalId(job.getJobExternalId())
                .compatibilityScore(job.getCompatibilityScore())
                .build();
    }
}
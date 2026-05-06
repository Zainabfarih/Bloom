package com.bloom.jobservice.service;

import com.bloom.jobservice.dto.SaveJobRequest;
import com.bloom.jobservice.dto.SavedJobResponse;
import com.bloom.jobservice.dto.SkillsDTO;
import com.bloom.jobservice.entity.SavedJob;
import com.bloom.jobservice.external.CvServiceClient;
import com.bloom.jobservice.mapper.SavedJobMapper;
import com.bloom.jobservice.repository.SavedJobRepository;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SavedJobServiceTest {

    @Mock
    private SavedJobRepository savedJobRepository;
    @Mock
    private SavedJobMapper savedJobMapper;
    @Mock
    private SkillMatchingService skillMatchingService;
    @Mock
    private CvServiceClient cvServiceClient;

    @InjectMocks
    private SavedJobService savedJobService;

    private SaveJobRequest request;
    private final Long userId = 1L;
    private final String bearerToken = "Bearer test-token";

    @BeforeEach
    void setUp() {
        request = new SaveJobRequest();
        request.setJobExternalId("ext-123");
        request.setJobTitle("Java Developer");
        request.setJobCompany("TechCorp");
        request.setRequiredSkills(List.of("Java", "Docker"));
    }

    @Test
    void save_job_computes_matching_and_persists() {
        // Given
        SkillsDTO skills = new SkillsDTO();
        skills.setSkills(List.of("java", "python"));

        SavedJob entity = SavedJob.builder()
                .id(1L).uuid(UUID.randomUUID())
                .userId(userId)
                .jobExternalId("ext-123")
                .jobTitle("Java Developer")
                .savedAt(Instant.now())
                .build();

        SavedJobResponse expectedResponse = SavedJobResponse.builder()
                .uuid(entity.getUuid())
                .jobExternalId("ext-123")
                .jobTitle("Java Developer")
                .compatibilityScore(50)
                .build();

        when(savedJobRepository.findByUserIdAndJobExternalId(userId, "ext-123"))
                .thenReturn(Optional.empty());
        when(cvServiceClient.getUserSkills(userId, bearerToken))
                .thenReturn(skills);
        when(skillMatchingService.findMatched(any(), any()))
                .thenReturn(List.of("Java"));
        when(skillMatchingService.findMissing(any(), any()))
                .thenReturn(List.of("Docker"));
        when(skillMatchingService.computeScore(any(), any()))
                .thenReturn(50);
        when(savedJobMapper.toEntity(any(), anyLong()))
                .thenReturn(entity);
        when(savedJobRepository.save(entity))
                .thenReturn(entity);
        when(savedJobMapper.toResponse(entity))
                .thenReturn(expectedResponse);

        // When
        SavedJobResponse result = savedJobService.saveJob(
                userId, request, bearerToken);

        // Then
        assertThat(result.getCompatibilityScore()).isEqualTo(50);
        verify(savedJobRepository, times(1)).save(any());
        verify(skillMatchingService).computeScore(any(), any());
    }

    @Test
    void save_job_is_idempotent_when_already_saved() {
        // Given
        SavedJob existing = SavedJob.builder()
                .id(1L).uuid(UUID.randomUUID())
                .userId(userId)
                .jobExternalId("ext-123")
                .build();

        SavedJobResponse existingResponse = SavedJobResponse.builder()
                .uuid(existing.getUuid())
                .jobExternalId("ext-123")
                .build();

        when(savedJobRepository.findByUserIdAndJobExternalId(userId, "ext-123"))
                .thenReturn(Optional.of(existing));
        when(savedJobMapper.toResponse(existing))
                .thenReturn(existingResponse);

        // When
        SavedJobResponse result = savedJobService.saveJob(
                userId, request, bearerToken);

        // Then
        assertThat(result.getJobExternalId()).isEqualTo("ext-123");
        // Pas de nouvel appel à save()
        verify(savedJobRepository, never()).save(any());
        verify(cvServiceClient, never()).getUserSkills(anyLong(), anyString());
    }

    @Test
    void save_job_succeeds_even_when_cv_service_is_down() {
        // Given
        when(savedJobRepository.findByUserIdAndJobExternalId(userId, "ext-123"))
                .thenReturn(Optional.empty());
        when(cvServiceClient.getUserSkills(userId, bearerToken))
                .thenThrow(new RuntimeException("cv-service unavailable"));
        when(skillMatchingService.findMatched(any(), any()))
                .thenReturn(List.of());
        when(skillMatchingService.findMissing(any(), any()))
                .thenReturn(List.of());
        when(skillMatchingService.computeScore(any(), any()))
                .thenReturn(0);

        SavedJob entity = SavedJob.builder()
                .id(1L).uuid(UUID.randomUUID())
                .userId(userId)
                .jobExternalId("ext-123")
                .compatibilityScore(0)
                .savedAt(Instant.now())
                .build();

        when(savedJobMapper.toEntity(any(), anyLong())).thenReturn(entity);
        when(savedJobRepository.save(entity)).thenReturn(entity);
        when(savedJobMapper.toResponse(entity)).thenReturn(
                SavedJobResponse.builder()
                        .uuid(entity.getUuid())
                        .compatibilityScore(0)
                        .build()
        );

        // When — ne doit pas lever d'exception
        SavedJobResponse result = savedJobService.saveJob(
                userId, request, bearerToken);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getCompatibilityScore()).isEqualTo(0);
        verify(savedJobRepository).save(any());
    }
}
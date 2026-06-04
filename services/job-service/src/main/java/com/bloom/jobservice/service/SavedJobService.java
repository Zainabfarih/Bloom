package com.bloom.jobservice.service;

import com.bloom.jobservice.dto.SaveJobRequest;
import com.bloom.jobservice.dto.SavedJobResponse;
import com.bloom.jobservice.dto.SkillGapResponse;
import com.bloom.jobservice.dto.SkillsDTO;
import com.bloom.jobservice.entity.SavedJob;
import com.bloom.jobservice.entity.SkillType;
import com.bloom.jobservice.exception.JobsApiException;
import com.bloom.jobservice.exception.ResourceNotFoundException;
import com.bloom.jobservice.external.CvServiceClient;
import com.bloom.jobservice.mapper.SavedJobMapper;
import com.bloom.jobservice.repository.SavedJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SavedJobService {

    private final SavedJobRepository  savedJobRepository;
    private final SkillMatchingService skillMatchingService;
    private final CvServiceClient      cvServiceClient;
    private final SavedJobMapper       savedJobMapper;

    @Transactional
    public SavedJobResponse saveJob(Long userId, SaveJobRequest request, String bearerToken) {
        return savedJobRepository
                .findByUserIdAndJobExternalId(userId, request.getJobExternalId())
                .map(savedJobMapper::toResponse)
                .orElseGet(() -> persistNewSavedJob(userId, request, bearerToken));
    }

    @Transactional(readOnly = true)
    public List<SavedJobResponse> getSavedJobs(Long userId) {
        return savedJobRepository
                .findByUserIdWithSkills(userId)
                .stream()
                .map(savedJobMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public SavedJobResponse getByUuid(Long userId, UUID uuid) {
        return savedJobRepository.findByUuidWithSkills(uuid)
                .filter(j -> j.getUserId().equals(userId))
                .map(savedJobMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Saved job not found: " + uuid));
    }

    @Transactional
    public void removeSavedJob(Long userId, String jobExternalId) {
        int deleted = savedJobRepository.deleteByUserIdAndJobExternalId(userId, jobExternalId);
        if (deleted == 0) {
            throw new ResourceNotFoundException("Saved job not found for user " + userId);
        }
    }

    @Transactional(readOnly = true)
    public SkillGapResponse getSkillGap(Long userId, Long targetJobId) {
        // targetJobId dans BLOOM est le Long id de SavedJob (pas jobExternalId)
        return savedJobRepository.findById(targetJobId)
                .filter(j -> j.getUserId().equals(userId))
                .map(j -> new SkillGapResponse(
                        userId,
                        targetJobId,
                        j.getJobTitle(),
                        j.getSkillsByType(SkillType.MISSING)
                ))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "SavedJob not found: id=" + targetJobId + " for userId=" + userId));
    }

    private SavedJobResponse persistNewSavedJob(Long userId, SaveJobRequest request, String bearerToken) {
        SkillsDTO cvData = fetchCvSkillsOrThrow(userId, request.getCvUuid(), bearerToken);

        List<String> cvSkills = cvData.getSkills() != null ? cvData.getSkills() : List.of();
        List<String> required = request.getRequiredSkills() != null ? request.getRequiredSkills() : List.of();

        request.setCvUuid(cvData.getCvUuid());
        request.setMatchedSkills(skillMatchingService.findMatched(required, cvSkills));
        request.setMissingSkills(skillMatchingService.findMissing(required, cvSkills));
        request.setCompatibilityScore(skillMatchingService.computeScore(required, cvSkills));

        SavedJob saved = savedJobRepository.save(savedJobMapper.toEntity(request, userId));

        log.info("Job saved — userId={} jobId={} cvUuid={} score={}%",
                userId, request.getJobExternalId(), request.getCvUuid(), request.getCompatibilityScore());

        return savedJobMapper.toResponse(saved);
    }

    private SkillsDTO fetchCvSkillsOrThrow(Long userId, UUID cvUuid, String bearerToken) {
        try {
            if (cvUuid != null) {
                log.debug("Fetching skills for specific CV — cvUuid={}", cvUuid);
                return cvServiceClient.getCvSkills(cvUuid, bearerToken);
            } else {
                log.debug("Fetching active CV skills — userId={}", userId);
                return cvServiceClient.getUserSkills(userId, bearerToken);
            }
        } catch (Exception e) {
            log.error("cv-service unavailable — userId={} cvUuid={} error={}", userId, cvUuid, e.getMessage());
            throw new JobsApiException(
                    "cv-service indisponible. Assurez-vous d'avoir téléchargé votre CV avant de sauvegarder un emploi."
            );
        }
    }
}
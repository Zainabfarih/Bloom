package com.bloom.jobservice.service;

import com.bloom.jobservice.dto.JobDetailResponse;
import com.bloom.jobservice.dto.SavedJobResponse;
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

    private final SavedJobRepository   savedJobRepository;
    private final SkillMatchingService skillMatchingService;
    private final CvServiceClient      cvServiceClient;
    private final JobService           jobService;
    private final SavedJobMapper       savedJobMapper;

    /**
     * Sauvegarde un job déjà présent en cache (via /search), identifié par {@code jobId}.
     * Détails et skills requis récupérés server-side ; matching contre le CV actif
     * (ou {@code cvUuid} si fourni).
     */
    @Transactional
    public SavedJobResponse saveJob(Long userId, String jobId, UUID cvUuid, String bearerToken) {
        var existing = savedJobRepository.findByUserIdAndJobExternalId(userId, jobId);
        if (existing.isPresent()) {
            return savedJobMapper.toResponse(existing.get()); // idempotent
        }

        JobDetailResponse job = jobService.getJobForSave(jobId);
        List<String> required = job.getExtractedSkills() != null ? job.getExtractedSkills() : List.of();

        SkillsDTO cvData = fetchCvSkillsOrThrow(userId, cvUuid, bearerToken);
        List<String> cvSkills = cvData.getSkills() != null ? cvData.getSkills() : List.of();

        List<String> matched = skillMatchingService.findMatched(required, cvSkills);
        List<String> missing = skillMatchingService.findMissing(required, cvSkills);
        int score            = skillMatchingService.computeScore(required, cvSkills);

        String applyUrl = (job.getApplyOptions() != null && !job.getApplyOptions().isEmpty())
                ? job.getApplyOptions().get(0).getLink() : null;

        SavedJob entity = SavedJob.builder()
                .userId(userId)
                .cvUuid(cvData.getCvUuid())
                .jobExternalId(jobId)
                .jobTitle(job.getTitle())
                .jobCompany(job.getCompanyName())
                .jobLocation(job.getLocation())
                .jobApplyUrl(applyUrl)
                .compatibilityScore(score)
                .build();
        entity.addSkills(required, SkillType.REQUIRED);
        entity.addSkills(matched,  SkillType.MATCHED);
        entity.addSkills(missing,  SkillType.MISSING);

        SavedJob saved = savedJobRepository.save(entity);
        log.info("Job saved — userId={} jobId={} cvUuid={} score={}%",
                userId, jobId, cvData.getCvUuid(), score);

        return savedJobMapper.toResponse(saved);
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

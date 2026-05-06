package com.bloom.jobservice.service;

import com.bloom.jobservice.dto.SkillsDTO;
import com.bloom.jobservice.dto.SaveJobRequest;
import com.bloom.jobservice.dto.SavedJobResponse;
import com.bloom.jobservice.entity.SavedJob;
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

    private final SavedJobRepository savedJobRepository;
    private final SavedJobMapper savedJobMapper;
    private final SkillMatchingService skillMatchingService;
    private final CvServiceClient cvServiceClient;

    @Transactional
    public SavedJobResponse saveJob(Long userId,
                                    SaveJobRequest request,
                                    String bearerToken) {
        // Idempotent — si déjà sauvegardé, retourner l'existant
        return savedJobRepository
                .findByUserIdAndJobExternalId(userId, request.getJobExternalId())
                .map(SavedJobMapper::toResponse)
                .orElseGet(() -> {

                    // Récupérer les skills depuis cv-service
                    List<String> userSkills = fetchUserSkills(
                            userId, bearerToken);

                    // Calculer le matching
                    List<String> required = request.getRequiredSkills() != null
                            ? request.getRequiredSkills() : List.of();

                    request.setMatchedSkills(
                            skillMatchingService.findMatched(required, userSkills));
                    request.setMissingSkills(
                            skillMatchingService.findMissing(required, userSkills));
                    request.setCompatibilityScore(
                            skillMatchingService.computeScore(required, userSkills));

                    SavedJob job = savedJobRepository.save(
                            savedJobMapper.toEntity(request, userId));

                    return SavedJobMapper.toResponse(job);
                });
    }

    @Transactional(readOnly = true)
    public List<SavedJobResponse> getSavedJobs(Long userId) {
        return savedJobRepository
                .findByUserIdOrderByCompatibilityScoreDesc(userId)
                .stream()
                .map(SavedJobMapper::toResponse)
                .toList();
    }

    @Transactional
    public void removeSavedJob(Long userId, String jobExternalId) {
        if (!savedJobRepository
                .existsByUserIdAndJobExternalId(userId, jobExternalId)) {
            throw new ResourceNotFoundException("User job not found");
        }
        savedJobRepository
                .deleteByUserIdAndJobExternalId(userId, jobExternalId);
    }

    @Transactional(readOnly = true)
    public SavedJobResponse getByUuid(Long userId, UUID uuid) {
        return savedJobRepository.findByUuid(uuid)
                .filter(j -> j.getUserId().equals(userId))
                .map(SavedJobMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Saved job not found: " + uuid));
    }

    private List<String> fetchUserSkills(Long userId, String bearerToken) {
        try {
            SkillsDTO cv = cvServiceClient.getUserSkills(userId, bearerToken);
            return cv.getSkills() != null ? cv.getSkills() : List.of();
        } catch (Exception e) {
            // cv-service down → sauvegarde sans score, pas d'échec total
            log.warn("cv-service unavailable, saving without skill matching: {}",
                    e.getMessage());
            return List.of();
        }
    }
}
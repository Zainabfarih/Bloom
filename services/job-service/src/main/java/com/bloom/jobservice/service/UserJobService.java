package com.bloom.jobservice.service;

import com.bloom.jobservice.dto.SkillsDTO;
import com.bloom.jobservice.dto.SaveJobRequest;
import com.bloom.jobservice.dto.SavedJobResponse;
import com.bloom.jobservice.entity.UserJob;
import com.bloom.jobservice.exception.ResourceNotFoundException;
import com.bloom.jobservice.external.CvServiceClient;
import com.bloom.jobservice.mapper.UserJobMapper;
import com.bloom.jobservice.repository.UserJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserJobService {

    private final UserJobRepository userJobRepository;
    private final UserJobMapper userJobMapper;
    private final SkillMatchingService skillMatchingService;
    private final CvServiceClient cvServiceClient;

    @Transactional
    public SavedJobResponse saveJob(Long userId,
                                    SaveJobRequest request,
                                    String bearerToken) {
        // Idempotent — si déjà sauvegardé, retourner l'existant
        return userJobRepository
                .findByUserIdAndJobExternalId(userId, request.getJobExternalId())
                .map(UserJobMapper::toResponse)
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

                    UserJob job = userJobRepository.save(
                            userJobMapper.toEntity(request, userId));

                    return UserJobMapper.toResponse(job);
                });
    }

    @Transactional(readOnly = true)
    public List<SavedJobResponse> getUserJobs(Long studentId) {
        return userJobRepository
                .findByUserIdOrderByCompatibilityScoreDesc(studentId)
                .stream()
                .map(UserJobMapper::toResponse)
                .toList();
    }

    @Transactional
    public void removeSavedJob(Long studentId, String jobExternalId) {
        if (!userJobRepository
                .existsByUserIdAndJobExternalId(studentId, jobExternalId)) {
            throw new ResourceNotFoundException("User job not found");
        }
        userJobRepository
                .deleteByUserIdAndJobExternalId(studentId, jobExternalId);
    }

    @Transactional(readOnly = true)
    public SavedJobResponse getByUuid(Long userId, UUID uuid) {
        return userJobRepository.findByUuid(uuid)
                .filter(j -> j.getUserId().equals(userId))
                .map(UserJobMapper::toResponse)
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
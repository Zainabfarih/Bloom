package com.bloom.jobservice.service;

import com.bloom.jobservice.dto.SaveJobRequest;
import com.bloom.jobservice.dto.SavedJobResponse;
import com.bloom.jobservice.dto.SkillsDTO;
import com.bloom.jobservice.entity.SavedJob;
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

/**
 * SavedJobService — gestion des offres favorites d'un étudiant.
 *
 * Architecture CV :
 *   - L'étudiant uploade son CV (PDF) dans cv-service
 *   - cv-service extrait les skills du PDF (NLP/ML)
 *   - job-service appelle cv-service pour récupérer ces skills
 *   - job-service calcule le matching skills offre ↔ skills CV
 *
 * cv_uuid est NOT NULL en base : on ne sauvegarde un job QUE si
 * cv-service a pu fournir les skills du CV (matching obligatoire).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SavedJobService {

    private final SavedJobRepository   savedJobRepository;
    private final SavedJobMapper       savedJobMapper;
    private final SkillMatchingService skillMatchingService;
    private final CvServiceClient      cvServiceClient;

    /**
     * Sauvegarde un job dans les favoris de l'étudiant.
     * Idempotent : si déjà sauvegardé, retourne l'existant sans doublon.
     *
     * @param userId      ID de l'étudiant (depuis header X-User-Id)
     * @param request     Détails du job + cvUuid optionnel
     * @param bearerToken JWT transmis à cv-service (header Authorization)
     */
    @Transactional
    public SavedJobResponse saveJob(Long userId, SaveJobRequest request, String bearerToken) {
        return savedJobRepository
                .findByUserIdAndJobExternalId(userId, request.getJobExternalId())
                .map(savedJobMapper::toResponse)
                .orElseGet(() -> persistNewSavedJob(userId, request, bearerToken));
    }

    /**
     * Retourne tous les jobs sauvegardés de l'étudiant,
     * triés par score de compatibilité décroissant.
     */
    @Transactional(readOnly = true)
    public List<SavedJobResponse> getSavedJobs(Long userId) {
        return savedJobRepository
                .findByUserIdWithSkills(userId)
                .stream()
                .map(savedJobMapper::toResponse)
                .toList();
    }

    /**
     * Récupère un job sauvegardé par son UUID.
     * Vérifie que le job appartient bien à userId (ownership check).
     * Consommé par roadmap-service.
     */
    @Transactional(readOnly = true)
    public SavedJobResponse getByUuid(Long userId, UUID uuid) {
        return savedJobRepository.findByUuidWithSkills(uuid)
                .filter(j -> j.getUserId().equals(userId))
                .map(savedJobMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Saved job not found: " + uuid));
    }

    /**
     * Supprime un job des favoris.
     * Retourne 404 si le job n'existe pas pour cet utilisateur.
     */
    @Transactional
    public void removeSavedJob(Long userId, String jobExternalId) {
        int deleted = savedJobRepository.deleteByUserIdAndJobExternalId(userId, jobExternalId);
        if (deleted == 0) {
            throw new ResourceNotFoundException("Saved job not found for user " + userId);
        }
    }

    // ── Private ────────────────────────────────────────────────────────

    private SavedJobResponse persistNewSavedJob(Long userId, SaveJobRequest request, String bearerToken) {
        // 1. Fetch skills depuis cv-service (OBLIGATOIRE — cv_uuid NOT NULL en DB)
        //    Le CV a été uploadé par l'étudiant dans cv-service qui en a extrait les skills
        SkillsDTO cvData = fetchCvSkillsOrThrow(userId, request.getCvUuid(), bearerToken);

        List<String> cvSkills = cvData.getSkills() != null ? cvData.getSkills() : List.of();

        // 2. Stocker le cvUuid retourné par cv-service (non null garanti)
        request.setCvUuid(cvData.getCvUuid());

        // 3. Calculer le matching : skills offre ↔ skills du CV de l'étudiant
        List<String> required = request.getRequiredSkills() != null
                ? request.getRequiredSkills() : List.of();

        request.setMatchedSkills(skillMatchingService.findMatched(required, cvSkills));
        request.setMissingSkills(skillMatchingService.findMissing(required, cvSkills));
        request.setCompatibilityScore(skillMatchingService.computeScore(required, cvSkills));

        // 4. Persister
        SavedJob saved = savedJobRepository.save(savedJobMapper.toEntity(request, userId));

        log.info("Job saved — userId={} jobId={} cvUuid={} score={}%",
                userId, request.getJobExternalId(), request.getCvUuid(), request.getCompatibilityScore());

        return savedJobMapper.toResponse(saved);
    }

    /**
     * Appelle cv-service pour récupérer les skills extraits du CV de l'étudiant.
     *
     * - Si cvUuid fourni dans la requête → endpoint CV spécifique
     * - Si cvUuid absent              → endpoint CV actif de l'utilisateur
     *
     * cv_uuid est NOT NULL en base : si cv-service est indisponible,
     * on lève une exception (pas de sauvegarde partielle sans matching).
     *
     * @throws JobsApiException si cv-service est indisponible
     */
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
            log.error("cv-service unavailable — userId={} cvUuid={} error={}",
                    userId, cvUuid, e.getMessage());
            throw new JobsApiException(
                    "cv-service indisponible. Impossible de calculer le skill matching. " +
                            "Assurez-vous d'avoir uploadé votre CV avant de sauvegarder un job."
            );
        }
    }
}
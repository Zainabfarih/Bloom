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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Logique métier des CV : upload PDF, création manuelle, lecture,
 * analyse ATS et suppression. Garantit un seul CV actif par utilisateur.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CvService {

    private final CvRepository      cvRepository;
    private final PdfTextExtractor  pdfTextExtractor;
    private final CvSkillExtractor  cvSkillExtractor;
    private final CvAnalysisService cvAnalysisService;
    private final CvMapper          cvMapper;

    /** Importe un CV PDF, en extrait les skills et le rend actif. */
    @Transactional
    public CvResponse uploadCv(Long userId, MultipartFile file, String title) {
        // Texte extrait uniquement pour en déduire les skills, non persisté
        String text = pdfTextExtractor.extract(file);
        List<String> skills = cvSkillExtractor.extract(text);

        byte[] fileData;
        try {
            fileData = file.getBytes();
        } catch (IOException e) {
            throw new CvProcessingException("Impossible de lire le fichier uploadé.", e);
        }

        deactivateExistingCvs(userId);

        Cv cv = Cv.builder()
                .userId(userId)
                .title(title != null && !title.isBlank() ? title.trim() : file.getOriginalFilename())
                .source(CvSource.UPLOAD)
                .originalFilename(file.getOriginalFilename())
                .contentType("application/pdf")
                .fileData(fileData)
                .fileSize((long) fileData.length)
                .active(true)
                .build();
        cv.replaceSkills(skills);

        Cv saved = cvRepository.save(cv);
        log.info("CV uploadé — userId={} cvUuid={} skills={} taille={}o",
                userId, saved.getUuid(), skills.size(), fileData.length);
        return cvMapper.toResponse(saved);
    }

    /** Crée un CV à partir des sections saisies ; stocke le PDF généré côté front. */
    @Transactional
    public CvResponse createManualCv(Long userId, ManualCvRequest request) {
        String description = assembleDescription(request);
        List<String> skills = request.getSkills();
        byte[] fileData = decodePdf(request.getPdfBase64());

        deactivateExistingCvs(userId);

        String title = request.getTitle() != null && !request.getTitle().isBlank()
                ? request.getTitle().trim() : "CV (saisie manuelle)";

        Cv cv = Cv.builder()
                .userId(userId)
                .title(title)
                .source(CvSource.MANUAL)
                .originalFilename(slugify(title) + ".pdf")
                .contentType("application/pdf")
                .fileData(fileData)
                .fileSize((long) fileData.length)
                .description(description)
                .active(true)
                .build();
        cv.replaceSkills(skills);

        Cv saved = cvRepository.save(cv);
        log.info("CV manuel créé — userId={} cvUuid={} skills={}", userId, saved.getUuid(), skills.size());
        return cvMapper.toResponse(saved);
    }

    /** Retourne le CV actif de l'étudiant, ou 404 si aucun. */
    @Transactional(readOnly = true)
    public CvResponse getActiveCv(Long userId) {
        return cvMapper.toResponse(findActiveOrThrow(userId));
    }

    /** Liste tous les CV de l'étudiant, du plus récent au plus ancien. */
    @Transactional(readOnly = true)
    public List<CvResponse> getMyCvs(Long userId) {
        return cvRepository.findByUserId(userId).stream()
                .map(cvMapper::toResponse)
                .toList();
    }

    // Skills du CV actif, consommé par job-service (Feign)
    @Transactional(readOnly = true)
    public SkillsDTO getUserSkills(Long userId) {
        return cvMapper.toSkillsDTO(findActiveOrThrow(userId));
    }

    // Skills d'un CV précis par UUID, consommé par job-service (Feign)
    @Transactional(readOnly = true)
    public SkillsDTO getCvSkills(UUID cvUuid) {
        Cv cv = cvRepository.findByUuidWithSkills(cvUuid)
                .orElseThrow(() -> new ResourceNotFoundException("CV introuvable: " + cvUuid));
        return cvMapper.toSkillsDTO(cv);
    }

    /** Retourne l'entité CV si elle appartient à l'utilisateur (pour servir le fichier). */
    @Transactional(readOnly = true)
    public Cv getOwnedCv(Long userId, UUID cvUuid) {
        return cvRepository.findByUuid(cvUuid)
                .filter(c -> c.getUserId().equals(userId))
                .orElseThrow(() -> new ResourceNotFoundException("CV introuvable: " + cvUuid));
    }

    /** Analyse ATS du CV (score, points forts, problèmes) calculée à la volée. */
    @Transactional(readOnly = true)
    public CvAnalysisResponse analyzeCv(Long userId, UUID cvUuid) {
        Cv cv = cvRepository.findByUuidWithSkills(cvUuid)
                .filter(c -> c.getUserId().equals(userId))
                .orElseThrow(() -> new ResourceNotFoundException("CV introuvable: " + cvUuid));

        // CV manuel : description ; CV uploadé : texte ré-extrait du PDF stocké
        String text = (cv.getDescription() != null && !cv.getDescription().isBlank())
                ? cv.getDescription()
                : pdfTextExtractor.extract(cv.getFileData());

        String analysisInput = text;
        List<String> skills = cv.getSkillNames();
        if (!skills.isEmpty()) {
            analysisInput += "\n\nSKILLS\n" + String.join(", ", skills);
        }

        CvAnalysisResponse analysis = cvAnalysisService.analyze(analysisInput);
        analysis.setCvUuid(cv.getUuid());
        return analysis;
    }

    /** Supprime un CV ; si c'était l'actif, promeut le plus récent restant. */
    @Transactional
    public void deleteCv(Long userId, UUID cvUuid) {
        Cv cv = cvRepository.findByUuid(cvUuid)
                .filter(c -> c.getUserId().equals(userId))
                .orElseThrow(() -> new ResourceNotFoundException("CV introuvable: " + cvUuid));

        boolean wasActive = cv.isActive();
        cvRepository.delete(cv);
        cvRepository.flush(); // libère l'index unique avant de réactiver un autre CV

        // Si le CV actif est supprimé, le plus récent restant redevient actif
        if (wasActive) {
            cvRepository.findByUserId(userId).stream()
                    .filter(c -> !c.getUuid().equals(cvUuid))
                    .findFirst()
                    .ifPresent(mostRecent -> {
                        mostRecent.setActive(true);
                        cvRepository.save(mostRecent);
                        log.info("CV actif promu — userId={} nouveauCvActif={}", userId, mostRecent.getUuid());
                    });
        }

        log.info("CV supprimé — userId={} cvUuid={} (étaitActif={})", userId, cvUuid, wasActive);
    }

    private byte[] decodePdf(String pdfBase64) {
        String data = pdfBase64.contains(",")
                ? pdfBase64.substring(pdfBase64.indexOf(',') + 1)
                : pdfBase64;
        try {
            byte[] bytes = Base64.getDecoder().decode(data);
            if (bytes.length == 0) {
                throw new CvProcessingException("Le PDF généré est vide.");
            }
            return bytes;
        } catch (IllegalArgumentException e) {
            throw new CvProcessingException("PDF base64 invalide.", e);
        }
    }

    private Cv findActiveOrThrow(Long userId) {
        return cvRepository.findActiveByUserIdWithSkills(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Aucun CV actif pour l'utilisateur " + userId +
                        ". Téléchargez ou créez un CV d'abord."));
    }

    private void deactivateExistingCvs(Long userId) {
        cvRepository.findByUserIdAndActiveTrue(userId).ifPresent(existing -> {
            existing.setActive(false);
            cvRepository.saveAndFlush(existing);
        });
    }

    private String assembleDescription(ManualCvRequest req) {
        StringBuilder sb = new StringBuilder();

        if (req.getSummary() != null && !req.getSummary().isBlank()) {
            sb.append("SUMMARY\n").append(req.getSummary().trim()).append("\n\n");
        }
        if (req.getExperiences() != null && !req.getExperiences().isEmpty()) {
            sb.append("EXPERIENCE\n")
              .append(req.getExperiences().stream()
                      .filter(e -> e != null && !e.isBlank())
                      .map(String::trim)
                      .collect(Collectors.joining("\n")))
              .append("\n\n");
        }
        if (req.getEducations() != null && !req.getEducations().isEmpty()) {
            sb.append("EDUCATION\n")
              .append(req.getEducations().stream()
                      .filter(e -> e != null && !e.isBlank())
                      .map(String::trim)
                      .collect(Collectors.joining("\n")))
              .append("\n\n");
        }
        if (req.getSkills() != null && !req.getSkills().isEmpty()) {
            sb.append("SKILLS\n").append(String.join(", ", req.getSkills())).append("\n");
        }

        return sb.toString().trim();
    }

    private String slugify(String input) {
        String slug = input.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        return slug.isBlank() ? "cv" : slug;
    }
}

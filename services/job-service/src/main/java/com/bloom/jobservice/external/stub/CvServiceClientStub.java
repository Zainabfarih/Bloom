package com.bloom.jobservice.external.stub;

import com.bloom.jobservice.dto.SkillsDTO;
import com.bloom.jobservice.external.CvServiceClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Stub de CvServiceClient pour le profil "dev-local".
 *
 * Permet de tester job-service SANS que cv-service soit démarré.
 * Simule un étudiant ayant uploadé un CV avec les skills ci-dessous.
 *
 * ACTIVATION :
 *   Dans application.yaml (ou via IntelliJ Run Config) :
 *     spring.profiles.active: dev-local
 *
 * DÉSACTIVER en prod/staging : ne pas activer le profil dev-local.
 */
@Profile("dev-local")
@Primary
@Component
@Slf4j
public class CvServiceClientStub implements CvServiceClient {

    // UUID fixe simulant un CV uploadé dans cv-service
    private static final UUID STUB_CV_UUID =
            UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

    // Skills simulés extraits du CV PDF par cv-service
    private static final List<String> STUB_CV_SKILLS = List.of(
            "Java", "Spring Boot", "Spring Security", "PostgreSQL",
            "Docker", "Git", "REST API", "Maven"
    );

    @Override
    public SkillsDTO getUserSkills(Long userId, String bearerToken) {
        log.warn("[STUB] cv-service not running — returning mock skills for userId={}", userId);
        return buildStubResponse(userId);
    }

    @Override
    public SkillsDTO getCvSkills(UUID cvUuid, String bearerToken) {
        log.warn("[STUB] cv-service not running — returning mock skills for cvUuid={}", cvUuid);
        // Utiliser le cvUuid demandé (pas le stub UUID) pour que le matching soit tracé correctement
        SkillsDTO dto = new SkillsDTO();
        dto.setUserId(null);
        dto.setCvUuid(cvUuid);
        dto.setSkills(STUB_CV_SKILLS);
        return dto;
    }

    private SkillsDTO buildStubResponse(Long userId) {
        SkillsDTO dto = new SkillsDTO();
        dto.setUserId(userId);
        dto.setCvUuid(STUB_CV_UUID);
        dto.setSkills(STUB_CV_SKILLS);
        return dto;
    }
}
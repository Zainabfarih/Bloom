package com.bloom.jobservice.external;

import com.bloom.jobservice.dto.SkillsDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.UUID;

/**
 * Client Feign vers cv-service.
 *
 * cv-service expose les skills EXTRAITS du CV uploadé par l'étudiant.
 * Il n'y a pas de "user skills" dans user-service — tout passe par le CV.
 *
 * Deux cas d'usage :
 *  1. L'étudiant ne précise pas de CV → on prend son CV actif (le dernier uploadé)
 *  2. L'étudiant précise un cvUuid → on prend ce CV spécifique
 */
@FeignClient(name = "cv-service")
public interface CvServiceClient {

    /**
     * Récupère les skills du CV actif de l'utilisateur.
     * Appelé quand cvUuid n'est PAS fourni dans SaveJobRequest.
     *
     * @param userId      ID de l'étudiant (depuis X-User-Id)
     * @param bearerToken JWT transmis tel quel depuis la requête entrante
     */
    @GetMapping("/api/cv/users/{userId}/skills")
    SkillsDTO getUserSkills(
            @PathVariable("userId") Long userId,
            @RequestHeader("Authorization") String bearerToken
    );

    /**
     * Récupère les skills d'un CV spécifique via son UUID.
     * Appelé quand cvUuid EST fourni dans SaveJobRequest.
     *
     * @param cvUuid      UUID du CV cible
     * @param bearerToken JWT transmis tel quel depuis la requête entrante
     */
    @GetMapping("/api/cv/{cvUuid}/skills")
    SkillsDTO getCvSkills(
            @PathVariable("cvUuid") UUID cvUuid,
            @RequestHeader("Authorization") String bearerToken
    );
}
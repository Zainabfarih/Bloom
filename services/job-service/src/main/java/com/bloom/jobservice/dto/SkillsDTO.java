package com.bloom.jobservice.dto;

import lombok.Data;

import java.util.List;
import java.util.UUID;

/**
 * DTO retourné par cv-service quand job-service demande les skills d'un CV.
 *
 * Flow :
 *   Étudiant uploade PDF → cv-service extrait les skills du CV
 *   job-service appelle cv-service → reçoit ce DTO → calcule le matching
 *
 * Note : PAS de "user skills" côté user-service.
 *        Les skills viennent exclusivement du CV uploadé dans cv-service.
 */
@Data
public class SkillsDTO {
    // ID de l'étudiant propriétaire du CV
    private Long   userId;

    // UUID du CV dont les skills ont été extraits
    // Toujours non null dans la réponse de cv-service
    private UUID   cvUuid;

    // Skills extraits du PDF du CV par cv-service (NLP/ML)
    // Ex: ["Java", "Spring Boot", "PostgreSQL", "Docker", "Git"]
    private List<String> skills;
}
package com.bloom.jobservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class SaveJobRequest {

    @NotBlank(message = "jobExternalId is required")
    private String jobExternalId;

    @NotBlank(message = "jobTitle is required")
    @Size(max = 500)
    private String jobTitle;

    @Size(max = 255)
    private String jobCompany;

    @Size(max = 255)
    private String jobLocation;

    private String jobApplyUrl;

    // Skills de l'offre d'emploi (extraits via GET /api/job/{jobId})
    private List<String> requiredSkills;

    // UUID du CV à utiliser pour le matching (optionnel).
    // Si absent → job-service appelle cv-service pour récupérer le CV actif de l'user.
    // Si présent → job-service appelle cv-service pour ce CV spécifique.
    // Dans les deux cas, cv-service retourne les skills extraits du PDF uploadé.
    private UUID cvUuid;

    // ── Calculés server-side par SkillMatchingService ─────────────────
    // Ignorés si envoyés par le client dans le body
    @JsonIgnore
    private List<String> matchedSkills;

    @JsonIgnore
    private List<String> missingSkills;

    @JsonIgnore
    private Integer compatibilityScore;
}
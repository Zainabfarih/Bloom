package com.bloom.cvservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * Saisie manuelle d'un CV section par section (alternative à l'upload PDF).
 * Toutes les sections sont obligatoires : elles servent à générer un PDF ATS
 * (stocké) et, avec les skills, à l'analyse ATS à la volée.
 */
@Data
public class ManualCvRequest {

    @Size(max = 255)
    private String title;

    @NotBlank(message = "summary is required")
    private String summary;

    /** Expériences professionnelles décrites en texte libre. */
    @NotEmpty(message = "at least one experience is required")
    private List<@NotBlank(message = "experience entry cannot be blank") String> experiences;

    /** Formations / diplômes décrits en texte libre. */
    @NotEmpty(message = "at least one education entry is required")
    private List<@NotBlank(message = "education entry cannot be blank") String> educations;

    /** Compétences (au moins une, une par ligne). */
    @NotEmpty(message = "at least one skill is required")
    private List<@NotBlank(message = "skill cannot be blank") String> skills;
}

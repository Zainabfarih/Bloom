package com.bloom.cvservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * Saisie manuelle d'un CV section par section (alternative à l'upload PDF).
 * Le texte des sections est assemblé en {@code rawText} pour l'extraction de skills
 * et l'analyse ATS.
 */
@Data
public class ManualCvRequest {

    @Size(max = 255)
    private String title;

    @NotBlank(message = "summary is required for a manual CV")
    private String summary;

    /** Expériences professionnelles décrites en texte libre. */
    private List<String> experiences;

    /** Formations / diplômes décrits en texte libre. */
    private List<String> educations;

    /** Compétences saisies manuellement. Si vide, elles seront extraites du texte par l'IA. */
    private List<String> skills;
}

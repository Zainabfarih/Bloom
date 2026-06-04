package com.bloom.cvservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Résultat de l'analyse ATS d'un CV — calculé à la volée par l'IA, jamais persisté.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CvAnalysisResponse {

    private UUID cvUuid;

    /** Score ATS global sur 100. */
    private Integer atsScore;

    /** Synthèse globale de la qualité du CV. */
    private String summary;

    /** Points forts détectés. */
    private List<String> strengths;

    /** Problèmes détectés (grammaire, structure, formatage, contenu). */
    private List<CvAnalysisIssue> issues;
}

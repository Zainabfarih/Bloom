package com.bloom.cvservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Un problème individuel relevé lors de l'analyse ATS (non persisté).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CvAnalysisIssue {

    /** GRAMMAR · STRUCTURE · FORMATTING · CONTENT */
    private String type;

    /** LOW · MEDIUM · HIGH */
    private String severity;

    /** Description du problème. */
    private String message;

    /** Suggestion concrète d'amélioration. */
    private String suggestion;
}

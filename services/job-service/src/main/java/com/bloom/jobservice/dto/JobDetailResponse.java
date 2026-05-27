package com.bloom.jobservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * JobDetailResponse — DTO renvoyé par GET /api/job/{jobId}.
 *
 * extractedSkills  → skills extraits par Ollama (ou fallback HF).
 * fromSkillCache   → true si les skills viennent du cache Redis (2ème appel+).
 * fromSearchCache  → toujours true ici : le job vient forcément du cache search
 *                    (on ne re-appelle jamais SerpAPI depuis /detail).
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JobDetailResponse {

    private String jobId;
    private String title;
    private String companyName;
    private String location;
    private String description;
    private List<String> extensions;
    private List<JobResult.ApplyOption> applyOptions;

    // ── Extraction skills ────────────────────────────────────────────────────

    /** Skills techniques extraits par Ollama (ou fallback Hugging Face). */
    private List<String> extractedSkills;

    /**
     * true  → skills lus depuis le cache Redis  (Ollama NON sollicité).
     * false → skills fraîchement extraits par Ollama (premier accès à ce job).
     */
    private boolean fromSkillCache;

    /**
     * true  → le job lui-même vient du cache Redis search (cas normal).
     * false → réservé à un éventuel appel direct SerpAPI (non implémenté).
     */
    private boolean fromSearchCache;
}
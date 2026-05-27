package com.bloom.jobservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * JobResult — représente un résultat de recherche SerpAPI.
 *
 * DESIGN :
 *   - extractedSkills est ABSENT du résultat de /search (lazy extraction).
 *     Il n'est rempli que quand on accède à /api/job/{jobId} (detail).
 *   - Le champ description est stocké en cache mais n'est PAS exposé dans /search
 *     pour alléger la réponse. Il est exposé dans /api/job/{jobId} (JobDetailResponse).
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class JobResult {

    @JsonProperty("job_id")
    private String jobId;

    @JsonProperty("title")
    private String title;

    @JsonProperty("company_name")
    private String companyName;

    @JsonProperty("location")
    private String location;

    /**
     * Stocké dans le cache Redis pour l'extraction ultérieure des skills,
     * mais non exposé dans la liste de recherche (voir JobSearchResult DTO).
     */
    @JsonProperty("description")
    private String description;

    /**
     * Options de candidature retournées par SerpAPI.
     * Ex: [{ "title": "Postuler sur LinkedIn", "link": "https://..." }]
     */
    @JsonProperty("apply_options")
    private List<ApplyOption> applyOptions;

    /**
     * Tags textuels SerpAPI : type contrat, date publication, etc.
     * Ex: ["Full-time", "6 days ago", "No degree mentioned"]
     */
    @JsonProperty("extensions")
    private List<String> extensions;

    // ── Rempli uniquement sur l'endpoint /detail — null dans /search ──────────
    private List<String> extractedSkills;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ApplyOption {
        @JsonProperty("title")
        private String title;

        @JsonProperty("link")
        private String link;
    }
}

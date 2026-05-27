package com.bloom.jobservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * JobSearchResult — DTO exposé dans /api/job/search.
 *
 * Ne contient PAS :
 *   - description  (volumineuse, inutile pour la liste)
 *   - extractedSkills (extraction lazy — uniquement sur /api/job/{id})
 *
 * Contient :
 *   - jobId      → identifiant externe SerpAPI (utilisé pour GET /api/job/{id} et POST /saved)
 *   - title, companyName, location
 *   - extensions → ["Full-time", "6 days ago", "No degree mentioned"] tel quel depuis SerpAPI
 *   - applyOptions → liens de candidature directs
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JobSearchResult {

    private String jobId;
    private String title;
    private String companyName;
    private String location;
    private List<String> extensions;
    private List<JobResult.ApplyOption> applyOptions;

    public static JobSearchResult from(JobResult r) {
        return JobSearchResult.builder()
                .jobId(r.getJobId())
                .title(r.getTitle())
                .companyName(r.getCompanyName())
                .location(r.getLocation())
                .extensions(r.getExtensions())
                .applyOptions(r.getApplyOptions())
                .build();
    }
}

package com.bloom.jobservice.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Réponse de /api/job/search — liste allégée (sans description ni skills).
 */
@Data
@Builder
public class JobSearchResponse {
    private List<JobSearchResult> jobs;
    private boolean fromCache;
    private int totalResults;
}

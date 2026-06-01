package com.bloom.jobservice.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Réponse de /api/job/search - Liste.
 */
@Data
@Builder
public class JobSearchResponse {
    private List<JobSearchResult> jobs;
    private boolean fromCache;
    private int totalResults;
}

package com.bloom.jobservice.dto;


import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class JobSearchResponse {
    private List<JobResult> jobs;
    private boolean fromCache;
    private int totalResults;
}
package com.bloom.jobservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * JobSearchResult — DTO exposé dans /api/job/search.
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

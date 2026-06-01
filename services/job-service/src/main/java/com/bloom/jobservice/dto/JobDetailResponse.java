package com.bloom.jobservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.List;

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

    private List<String> extractedSkills;

    private boolean fromSkillCache;

    private boolean fromSearchCache;
}
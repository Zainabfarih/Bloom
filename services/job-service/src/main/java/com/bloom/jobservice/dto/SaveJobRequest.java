package com.bloom.jobservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class SaveJobRequest {

    @NotBlank(message = "jobExternalId is required")
    private String jobExternalId;

    @NotBlank(message = "jobTitle is required")
    @Size(max = 500)
    private String jobTitle;

    @Size(max = 255)
    private String jobCompany;

    @Size(max = 255)
    private String jobLocation;

    private String jobApplyUrl;

    private List<String> requiredSkills;

    private UUID cvUuid;

    @JsonIgnore
    private List<String> matchedSkills;

    @JsonIgnore
    private List<String> missingSkills;

    @JsonIgnore
    private Integer compatibilityScore;
}
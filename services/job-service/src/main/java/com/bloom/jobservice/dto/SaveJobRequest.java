package com.bloom.jobservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

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

    // Ces champs sont remplis par le service — pas par le frontend
    private List<String> matchedSkills;
    private List<String> missingSkills;
    private Integer compatibilityScore;
}
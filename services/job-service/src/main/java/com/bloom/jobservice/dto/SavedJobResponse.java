package com.bloom.jobservice.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class SavedJobResponse {
    private UUID    uuid;
    private String  jobExternalId;
    private String  jobTitle;
    private String  jobCompany;
    private String  jobLocation;
    private String  jobApplyUrl;

    // UUID du CV (cv-service) dont les skills ont été utilisés pour le matching
    private UUID    cvUuid;

    private List<String> requiredSkills;
    private List<String> matchedSkills;
    private List<String> missingSkills;
    private Integer      compatibilityScore;
    private Instant      savedAt;
}
package com.bloom.jobservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * JobResult — représente un résultat de recherche SerpAPI.
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


    @JsonProperty("description")
    private String description;


    @JsonProperty("apply_options")
    private List<ApplyOption> applyOptions;


    @JsonProperty("extensions")
    private List<String> extensions;

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

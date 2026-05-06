package com.bloom.jobservice.dto;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

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

    @JsonProperty("job_apply_link")
    private String applyLink;

    @JsonProperty("extensions")
    private List<String> extensions;
}
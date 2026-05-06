package com.bloom.jobservice.dto;




import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class JobResponse {

    @JsonProperty("jobs_results")
    private List<JobResult> jobsResults;

    @JsonProperty("error")
    private String error;
}
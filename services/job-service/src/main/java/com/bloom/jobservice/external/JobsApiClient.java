package com.bloom.jobservice.external;

import com.bloom.jobservice.dto.JobResponse;
import com.bloom.jobservice.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
        name          = "jobs-api",
        url           = "${jobsapi.base-url}",
        configuration = FeignConfig.class
)
public interface JobsApiClient {

    @GetMapping("/search")
    static JobResponse searchJobs(
            @RequestParam("engine") String engine,
            @RequestParam("q") String query,
            @RequestParam("location") String location,
            @RequestParam("api_key") String apiKey
    ) {
        return null;
    }
}
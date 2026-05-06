package com.bloom.jobservice.external;

import com.bloom.jobservice.dto.JobResponse;
import com.bloom.jobservice.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
        name          = "serp-api",
        url           = "${serpapi.base-url}",
        configuration = FeignConfig.class
)
public interface JobsApiClient {

    @GetMapping("/search")
    JobResponse searchJobs(
            @RequestParam("engine")   String engine,
            @RequestParam("q")        String query,
            @RequestParam("location") String location,
            @RequestParam("api_key")  String apiKey
    );
}
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

    // AJOUT DE .json ICI pour SerpApi
    @GetMapping("/search.json")
    JobResponse searchJobs(
            @RequestParam("engine")   String engine,   // Pour toi : "google_jobs"
            @RequestParam("q")        String query,    // ex: "Java Developer"
            @RequestParam("location") String location, // ex: "Paris, France"
            @RequestParam("api_key")  String apiKey    // Ta clé ${jobsapi.key}
    );
}

package com.bloom.roadmapservice.external;

import com.bloom.roadmapservice.dto.SkillGapResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "job-service", fallback = JobServiceClientFallback.class)
public interface JobServiceClient {

    @GetMapping("/api/job/skill-gap")
    SkillGapResponse getJobSkillGap(
            @RequestParam("userId") Long userId,
            @RequestParam("targetJobId") Long targetJobId,
            @RequestHeader("X-User-Id") String xUserId,           // requis par GatewayAuthFilter
            @RequestHeader("X-Gateway-Secret") String gatewaySecret
    );
}
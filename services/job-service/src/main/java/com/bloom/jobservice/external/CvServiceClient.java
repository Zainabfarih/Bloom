package com.bloom.jobservice.external;

import com.bloom.jobservice.dto.SkillsDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.UUID;

@FeignClient(name = "cv-service")
public interface CvServiceClient {

    @GetMapping("/api/cv/users/{userId}/skills")
    SkillsDTO getUserSkills(
            @PathVariable("userId") Long userId,
            @RequestHeader("Authorization") String bearerToken
    );

    @GetMapping("/api/cv/{cvUuid}/skills")
    SkillsDTO getCvSkills(
            @PathVariable("cvUuid") UUID cvUuid,
            @RequestHeader("Authorization") String bearerToken
    );
}
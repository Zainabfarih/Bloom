package com.bloom.jobservice.external;

import com.bloom.jobservice.dto.SkillsDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "cv-service")
public interface CvServiceClient {

    @GetMapping("/api/cv/users/{id}/skills")
    SkillsDTO getUserSkills(
            @PathVariable("userId") Long studentId,
            @RequestHeader("Authorization") String bearerToken
    );
}
package com.bloom.jobservice.mapper;

import com.bloom.jobservice.dto.SavedJobResponse;
import com.bloom.jobservice.entity.SavedJob;
import com.bloom.jobservice.entity.SkillType;
import org.springframework.stereotype.Component;

@Component
public class SavedJobMapper {

    public SavedJobResponse toResponse(SavedJob entity) {
        return SavedJobResponse.builder()
                .uuid(entity.getUuid())
                .jobExternalId(entity.getJobExternalId())
                .jobTitle(entity.getJobTitle())
                .jobCompany(entity.getJobCompany())
                .jobLocation(entity.getJobLocation())
                .jobApplyUrl(entity.getJobApplyUrl())
                .cvUuid(entity.getCvUuid())
                .requiredSkills(entity.getSkillsByType(SkillType.REQUIRED))
                .matchedSkills(entity.getSkillsByType(SkillType.MATCHED))
                .missingSkills(entity.getSkillsByType(SkillType.MISSING))
                .compatibilityScore(entity.getCompatibilityScore())
                .savedAt(entity.getSavedAt())
                .build();
    }
}

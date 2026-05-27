package com.bloom.jobservice.mapper;

import com.bloom.jobservice.dto.SaveJobRequest;
import com.bloom.jobservice.dto.SavedJobResponse;
import com.bloom.jobservice.entity.SavedJob;
import com.bloom.jobservice.entity.SkillType;
import org.springframework.stereotype.Component;

@Component
public class SavedJobMapper {

    /**
     * Convertit SaveJobRequest → SavedJob entity.
     *
     * À ce stade, req.getCvUuid() est GARANTI non null :
     * SavedJobService l'a rempli avec le cvUuid retourné par cv-service.
     */
    public SavedJob toEntity(SaveJobRequest req, Long userId) {
        SavedJob job = SavedJob.builder()
                .userId(userId)
                .cvUuid(req.getCvUuid())              // non null — vérifié par le service
                .jobExternalId(req.getJobExternalId())
                .jobTitle(req.getJobTitle())
                .jobCompany(req.getJobCompany())
                .jobLocation(req.getJobLocation())
                .jobApplyUrl(req.getJobApplyUrl())
                .compatibilityScore(req.getCompatibilityScore())
                .build();

        job.addSkills(req.getRequiredSkills(), SkillType.REQUIRED);
        job.addSkills(req.getMatchedSkills(),  SkillType.MATCHED);
        job.addSkills(req.getMissingSkills(),  SkillType.MISSING);

        return job;
    }

    /**
     * Convertit SavedJob entity → SavedJobResponse DTO.
     */
    public SavedJobResponse toResponse(SavedJob entity) {
        return SavedJobResponse.builder()
                .uuid(entity.getUuid())
                .jobExternalId(entity.getJobExternalId())
                .jobTitle(entity.getJobTitle())
                .jobCompany(entity.getJobCompany())
                .jobLocation(entity.getJobLocation())
                .jobApplyUrl(entity.getJobApplyUrl())
                .cvUuid(entity.getCvUuid())           // UUID du CV utilisé pour le matching
                .requiredSkills(entity.getSkillsByType(SkillType.REQUIRED))
                .matchedSkills(entity.getSkillsByType(SkillType.MATCHED))
                .missingSkills(entity.getSkillsByType(SkillType.MISSING))
                .compatibilityScore(entity.getCompatibilityScore())
                .savedAt(entity.getSavedAt())
                .build();
    }
}
package com.bloom.jobservice.mapper;

import com.bloom.jobservice.dto.SavedJobResponse;
import com.bloom.jobservice.entity.SavedJob;
import com.bloom.jobservice.entity.SkillType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SavedJobMapperTest {

    private final SavedJobMapper mapper = new SavedJobMapper();

    @Test
    void toResponse_maps_fields_and_skills_by_type() {
        SavedJob job = SavedJob.builder()
                .id(7L)
                .uuid(UUID.randomUUID())
                .userId(1L)
                .cvUuid(UUID.randomUUID())
                .jobExternalId("ext-1")
                .jobTitle("Java Developer")
                .jobCompany("TechCorp")
                .jobLocation("Rabat")
                .jobApplyUrl("https://apply")
                .compatibilityScore(67)
                .savedAt(Instant.now())
                .build();
        job.addSkills(List.of("Java", "Docker"), SkillType.REQUIRED);
        job.addSkills(List.of("Java"), SkillType.MATCHED);
        job.addSkills(List.of("Docker"), SkillType.MISSING);

        SavedJobResponse response = mapper.toResponse(job);

        assertThat(response.getId()).isEqualTo(7L);
        assertThat(response.getJobExternalId()).isEqualTo("ext-1");
        assertThat(response.getJobTitle()).isEqualTo("Java Developer");
        assertThat(response.getCompatibilityScore()).isEqualTo(67);
        assertThat(response.getRequiredSkills()).containsExactly("Java", "Docker");
        assertThat(response.getMatchedSkills()).containsExactly("Java");
        assertThat(response.getMissingSkills()).containsExactly("Docker");
    }
}

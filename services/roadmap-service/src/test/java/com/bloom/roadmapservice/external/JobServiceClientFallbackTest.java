package com.bloom.roadmapservice.external;

import com.bloom.roadmapservice.dto.SkillGapResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JobServiceClientFallbackTest {

    @Test
    @DisplayName("Fallback : retourne une SkillGapResponse vide pour ne pas casser le flux")
    void fallback_returns_empty_skill_gap() {
        JobServiceClientFallback fb = new JobServiceClientFallback();

        SkillGapResponse response = fb.getJobSkillGap(1L, 1001L, "1", "secret");

        assertThat(response.userId()).isEqualTo(1L);
        assertThat(response.jobId()).isEqualTo(1001L);
        assertThat(response.jobTitle()).isNull();
        assertThat(response.missingSkills()).isEmpty();
    }
}

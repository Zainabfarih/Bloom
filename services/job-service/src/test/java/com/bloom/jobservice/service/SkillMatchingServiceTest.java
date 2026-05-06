package com.bloom.jobservice.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SkillMatchingServiceTest {

    private final SkillMatchingService service = new SkillMatchingService();

    @Test
    void score_is_100_when_all_skills_matched() {
        var required = List.of("Java", "Spring Boot", "PostgreSQL");
        var student  = List.of("java", "spring-boot", "postgresql");
        assertThat(service.computeScore(required, student)).isEqualTo(100);
    }

    @Test
    void score_is_50_when_half_skills_matched() {
        var required = List.of("Java", "Docker");
        var student  = List.of("java");
        assertThat(service.computeScore(required, student)).isEqualTo(50);
    }

    @Test
    void score_is_0_when_no_skills_matched() {
        var required = List.of("Kubernetes", "Rust");
        var student  = List.of("java", "python");
        assertThat(service.computeScore(required, student)).isEqualTo(0);
    }

    @Test
    void score_is_0_when_required_is_empty() {
        assertThat(service.computeScore(List.of(), List.of("Java"))).isEqualTo(0);
    }

    @Test
    void normalization_handles_case_and_separators() {
        // "Spring Boot" == "spring-boot" == "springboot"
        var required = List.of("Spring Boot");
        var student  = List.of("springboot");
        assertThat(service.computeScore(required, student)).isEqualTo(100);
    }

    @Test
    void find_missing_returns_correct_skills() {
        var required = List.of("Java", "Docker", "Kubernetes");
        var student  = List.of("java");
        assertThat(service.findMissing(required, student))
                .containsExactlyInAnyOrder("Docker", "Kubernetes");
    }

    @Test
    void find_matched_returns_correct_skills() {
        var required = List.of("Java", "Docker");
        var student  = List.of("java", "python");
        assertThat(service.findMatched(required, student))
                .containsExactly("Java");
    }
}
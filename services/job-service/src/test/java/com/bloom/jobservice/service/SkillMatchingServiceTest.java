/* ============================================================
 *  FICHIER 1 : SkillMatchingServiceTest.java
 *  Tests unitaires purs — aucune dépendance Spring/Mockito
 *  Couvre : computeScore, findMatched, findMissing, normalisation
 * ============================================================ */
package com.bloom.jobservice.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SkillMatchingServiceTest {

    private final SkillMatchingService service = new SkillMatchingService();

    // ── computeScore ─────────────────────────────────────────

    @Test
    void score_is_100_when_all_required_skills_are_matched() {
        assertThat(service.computeScore(
                List.of("Java", "Spring Boot", "PostgreSQL"),
                List.of("java", "spring-boot", "postgresql")
        )).isEqualTo(100);
    }

    @Test
    void score_is_50_when_half_matched() {
        assertThat(service.computeScore(
                List.of("Java", "Docker"),
                List.of("java")
        )).isEqualTo(50);
    }

    @Test
    void score_is_0_when_nothing_matched() {
        assertThat(service.computeScore(
                List.of("Kubernetes", "Rust"),
                List.of("java", "python")
        )).isEqualTo(0);
    }

    @Test
    void score_is_0_when_required_is_empty() {
        // Logique actuelle : 0 si required vide. Cohérent avec "pas de critère"
        assertThat(service.computeScore(List.of(), List.of("Java"))).isEqualTo(0);
    }

    @Test
    void score_handles_null_student_skills() {
        // Ne doit pas lever NullPointerException
        assertThat(service.computeScore(List.of("Java"), null)).isEqualTo(0);
    }

    @Test
    void score_handles_null_required_skills() {
        assertThat(service.computeScore(null, List.of("Java"))).isEqualTo(0);
    }

    // ── Normalisation ─────────────────────────────────────────

    @Test
    void normalization_ignores_case_and_separators() {
        // "Spring Boot" == "spring-boot" == "springboot" après normalisation [^a-z0-9] → ""
        assertThat(service.computeScore(
                List.of("Spring Boot"),
                List.of("springboot")
        )).isEqualTo(100);
    }

    @Test
    void normalization_handles_dots_in_skill_names() {
        // "Node.js" → "nodejs"
        assertThat(service.computeScore(
                List.of("Node.js"),
                List.of("nodejs")
        )).isEqualTo(100);
    }

    // ── findMissing ───────────────────────────────────────────

    @Test
    void find_missing_returns_unmatched_required_skills() {
        assertThat(service.findMissing(
                List.of("Java", "Docker", "Kubernetes"),
                List.of("java")
        )).containsExactlyInAnyOrder("Docker", "Kubernetes");
    }

    @Test
    void find_missing_returns_empty_when_all_matched() {
        assertThat(service.findMissing(
                List.of("Java"),
                List.of("java", "python")
        )).isEmpty();
    }

    @Test
    void find_missing_returns_all_required_when_student_has_no_skills() {
        assertThat(service.findMissing(
                List.of("Java", "Docker"),
                List.of()
        )).containsExactlyInAnyOrder("Java", "Docker");
    }

    @Test
    void find_missing_handles_null_required() {
        assertThat(service.findMissing(null, List.of("Java"))).isEmpty();
    }

    // ── findMatched ───────────────────────────────────────────

    @Test
    void find_matched_returns_correct_skills() {
        assertThat(service.findMatched(
                List.of("Java", "Docker"),
                List.of("java", "python")
        )).containsExactly("Java");
    }

    @Test
    void find_matched_returns_empty_when_nothing_in_common() {
        assertThat(service.findMatched(
                List.of("Rust", "Haskell"),
                List.of("java")
        )).isEmpty();
    }

    @Test
    void find_matched_handles_null_required() {
        assertThat(service.findMatched(null, List.of("Java"))).isEmpty();
    }
}
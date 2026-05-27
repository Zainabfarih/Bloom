package com.bloom.jobservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JobSkillExtractorTest — teste uniquement les comportements sans LLM.
 *
 * Les @Value (geminiApiKey, hfModelUrl) sont injectés via ReflectionTestUtils
 * avec des valeurs fictives — aucun appel réseau n'est effectué car
 * extract("") / extract(null) court-circuitent avant tout appel HTTP.
 */
class JobSkillExtractorTest {

    private JobSkillExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new JobSkillExtractor();
        // Injection des @Value sans Spring context — évite NullPointerException
        // et garantit qu'aucune vraie clé API n'est utilisée dans les tests
        ReflectionTestUtils.setField(extractor, "geminiApiKey", "test-fake-key");
        ReflectionTestUtils.setField(extractor, "hfModelUrl", "http://localhost/fake-hf");
    }

    @Test
    void extract_returns_empty_for_blank_description() {
        assertThat(extractor.extract("")).isEmpty();
        assertThat(extractor.extract("   ")).isEmpty();
        assertThat(extractor.extract(null)).isEmpty();
    }
}
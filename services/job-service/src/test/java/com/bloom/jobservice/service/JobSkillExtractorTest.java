package com.bloom.jobservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class JobSkillExtractorTest {

    private JobSkillExtractor extractor;

    @BeforeEach
    void setUp() {
        ObjectMapper mapper = new ObjectMapper();
        extractor = new JobSkillExtractor(mapper);

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

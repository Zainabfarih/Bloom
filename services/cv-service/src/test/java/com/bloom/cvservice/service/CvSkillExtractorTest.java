package com.bloom.cvservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CvSkillExtractorTest {

    @Mock private HttpClient httpClient;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private CvSkillExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new CvSkillExtractor(objectMapper, httpClient);
        ReflectionTestUtils.setField(extractor, "geminiApiKey", "test-key");
        ReflectionTestUtils.setField(extractor, "hfModelUrl", "http://localhost/hf");
        ReflectionTestUtils.setField(extractor, "geminiBaseUrl", "http://localhost/gemini/");
    }

    @Test
    @DisplayName("Texte vide → liste vide sans appel réseau")
    void blank_text_returns_empty_without_http_call() throws Exception {
        assertThat(extractor.extract("  ")).isEmpty();
        verify(httpClient, never()).send(any(), any());
    }

    @Test
    @DisplayName("Gemini 200 → skills parsés et triés")
    void gemini_success_returns_sorted_skills() throws Exception {
        HttpResponse<String> resp = stringResponse(200, geminiEnvelope("{\"skills\":[\"Java\",\"Docker\"]}"));
        when(httpClient.<String>send(any(), any())).thenReturn(resp);

        List<String> skills = extractor.extract("CV avec Java et Docker");

        assertThat(skills).containsExactly("Docker", "Java");
    }

    @Test
    @DisplayName("Gemini non-200 → fallback Hugging Face")
    void gemini_failure_falls_back_to_huggingface() throws Exception {
        HttpResponse<String> gemini = stringResponse(500, "error");
        HttpResponse<String> hf = stringResponse(200, "{\"skills\":[\"Python\",\"SQL\"]}");
        when(httpClient.<String>send(any(), any())).thenReturn(gemini).thenReturn(hf);

        List<String> skills = extractor.extract("CV Python SQL");

        assertThat(skills).containsExactly("Python", "SQL");
        verify(httpClient, org.mockito.Mockito.times(2)).send(any(), any());
    }

    @Test
    @DisplayName("Gemini lève une exception → fallback Hugging Face")
    void gemini_exception_falls_back_to_huggingface() throws Exception {
        HttpResponse<String> hf = stringResponse(200, "{\"skills\":[\"React\"]}");
        when(httpClient.<String>send(any(), any()))
                .thenThrow(new IOException("network down"))
                .thenReturn(hf);

        assertThat(extractor.extract("CV React")).containsExactly("React");
    }

    @Test
    @DisplayName("Gemini ET Hugging Face en échec → liste vide")
    void both_providers_fail_returns_empty() throws Exception {
        HttpResponse<String> gemini = stringResponse(500, "error");
        HttpResponse<String> hf = stringResponse(503, "error");
        when(httpClient.<String>send(any(), any())).thenReturn(gemini).thenReturn(hf);

        assertThat(extractor.extract("CV")).isEmpty();
    }

    private String geminiEnvelope(String modelText) throws Exception {
        ObjectNode root = objectMapper.createObjectNode();
        ArrayNode candidates = root.putArray("candidates");
        ObjectNode content = candidates.addObject().putObject("content");
        ArrayNode parts = content.putArray("parts");
        parts.addObject().put("text", modelText);
        return objectMapper.writeValueAsString(root);
    }

    @SuppressWarnings("unchecked")
    private HttpResponse<String> stringResponse(int status, String body) {
        HttpResponse<String> resp = mock(HttpResponse.class);
        when(resp.statusCode()).thenReturn(status);
        if (status == 200) {
            when(resp.body()).thenReturn(body);
        }
        return resp;
    }
}

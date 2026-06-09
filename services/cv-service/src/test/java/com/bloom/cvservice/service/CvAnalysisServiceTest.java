package com.bloom.cvservice.service;

import com.bloom.cvservice.dto.CvAnalysisResponse;
import com.bloom.cvservice.exception.AiServiceException;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CvAnalysisServiceTest {

    @Mock private HttpClient httpClient;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private CvAnalysisService service;

    @BeforeEach
    void setUp() {
        service = new CvAnalysisService(objectMapper, httpClient);
        ReflectionTestUtils.setField(service, "geminiApiKey", "test-key");
        ReflectionTestUtils.setField(service, "geminiBaseUrl", "http://localhost/gemini/");
    }

    @Test
    @DisplayName("Texte vide → AiServiceException sans appel réseau")
    void blank_text_throws_without_http_call() throws Exception {
        assertThatThrownBy(() -> service.analyze("   "))
                .isInstanceOf(AiServiceException.class);
        verify(httpClient, never()).send(any(), any());
    }

    @Test
    @DisplayName("Gemini 200 → analyse parsée (score, points forts, problèmes)")
    void gemini_success_parses_analysis() throws Exception {
        String model = "{\"atsScore\":85,\"summary\":\"Bon CV\","
                + "\"strengths\":[\"Clair\",\"Structuré\"],"
                + "\"issues\":[{\"type\":\"CONTENT\",\"severity\":\"LOW\","
                + "\"message\":\"Ajouter des chiffres\",\"suggestion\":\"Quantifier\"}]}";
        HttpResponse<String> resp = stringResponse(200, geminiEnvelope(model));
        when(httpClient.<String>send(any(), any())).thenReturn(resp);

        CvAnalysisResponse result = service.analyze("texte du cv");

        assertThat(result.getAtsScore()).isEqualTo(85);
        assertThat(result.getSummary()).isEqualTo("Bon CV");
        assertThat(result.getStrengths()).containsExactly("Clair", "Structuré");
        assertThat(result.getIssues()).hasSize(1);
        assertThat(result.getIssues().get(0).getType()).isEqualTo("CONTENT");
    }

    @Test
    @DisplayName("Score hors bornes → ramené dans [0,100]")
    void out_of_range_score_is_clamped() throws Exception {
        String model = "{\"atsScore\":150,\"summary\":\"x\",\"strengths\":[],\"issues\":[]}";
        HttpResponse<String> resp = stringResponse(200, geminiEnvelope(model));
        when(httpClient.<String>send(any(), any())).thenReturn(resp);

        assertThat(service.analyze("cv").getAtsScore()).isEqualTo(100);
    }

    @Test
    @DisplayName("Gemini non-200 → AiServiceException")
    void gemini_non_200_throws() throws Exception {
        HttpResponse<String> resp = stringResponse(500, "boom");
        when(httpClient.<String>send(any(), any())).thenReturn(resp);

        assertThatThrownBy(() -> service.analyze("cv"))
                .isInstanceOf(AiServiceException.class)
                .hasMessageContaining("500");
    }

    @Test
    @DisplayName("Échec réseau → AiServiceException")
    void network_failure_throws() throws Exception {
        when(httpClient.<String>send(any(), any())).thenThrow(new IOException("down"));

        assertThatThrownBy(() -> service.analyze("cv"))
                .isInstanceOf(AiServiceException.class);
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
        when(resp.body()).thenReturn(body);
        return resp;
    }
}

package com.bloom.roadmapservice.service;

import com.bloom.roadmapservice.entity.RoadmapStep;
import com.bloom.roadmapservice.entity.StepStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Teste la logique de parsing/fallback de GeminiAiServiceImpl SANS appel réseau.
 * On invoque les méthodes privées via réflection — c'est le coeur métier qu'on
 * doit couvrir sans dépendre de l'API Gemini réelle.
 */
class GeminiAiServiceImplTest {

    private GeminiAiServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new GeminiAiServiceImpl(new ObjectMapper());
        ReflectionTestUtils.setField(service, "geminiApiKey", "fake-key");
        ReflectionTestUtils.setField(service, "model", "gemini-2.5-flash");
        ReflectionTestUtils.setField(service, "maxOutputTokens", 4096);
        ReflectionTestUtils.setField(service, "timeoutSeconds", 5);
    }

    @SuppressWarnings("unchecked")
    private List<RoadmapStep> parseResponse(String body, List<String> missingSkills) throws Exception {
        Method m = GeminiAiServiceImpl.class.getDeclaredMethod(
                "parseResponse", String.class, List.class);
        m.setAccessible(true);
        return (List<RoadmapStep>) m.invoke(service, body, missingSkills);
    }

    @SuppressWarnings("unchecked")
    private List<RoadmapStep> fallbackSteps(List<String> missingSkills) throws Exception {
        Method m = GeminiAiServiceImpl.class.getDeclaredMethod("fallbackSteps", List.class);
        m.setAccessible(true);
        return (List<RoadmapStep>) m.invoke(service, missingSkills);
    }

    private String wrapAsGeminiResponse(String innerJson) {
        return """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [
                      { "text": %s }
                    ]
                  }
                }
              ]
            }
            """.formatted(toJsonString(innerJson));
    }

    /** Encode 'innerJson' as a JSON string literal (with quotes + escapes). */
    private String toJsonString(String s) {
        try {
            return new ObjectMapper().writeValueAsString(s);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("parseResponse : JSON valide → liste de steps avec resources")
    void parses_valid_response() throws Exception {
        String inner = """
            {"steps":[
              {"title":"Learn React","description":"Hooks & components","orderIndex":1,
               "estimatedDuration":"2 weeks",
               "resources":[
                  {"title":"React Docs","url":"https://react.dev","type":"doc"},
                  {"title":"","url":"https://invalid","type":"video"}
               ]},
              {"title":"Master TypeScript","description":"Types","orderIndex":2,
               "estimatedDuration":"3 weeks","resources":[]}
            ]}
            """;

        List<RoadmapStep> steps = parseResponse(wrapAsGeminiResponse(inner), List.of("React", "TypeScript"));

        assertThat(steps).hasSize(2);
        assertThat(steps.get(0).getTitle()).isEqualTo("Learn React");
        assertThat(steps.get(0).getStatus()).isEqualTo(StepStatus.PENDING);
        assertThat(steps.get(0).getResources()).hasSize(1); // la resource invalide est filtrée
        assertThat(steps.get(0).getResources().iterator().next().getStep()).isEqualTo(steps.get(0));
        assertThat(steps.get(1).getResources()).isEmpty();
    }

    @Test
    @DisplayName("parseResponse : steps absent → fallback")
    void empty_steps_falls_back_to_skill_list() throws Exception {
        String body = wrapAsGeminiResponse("{\"steps\":[]}");

        List<RoadmapStep> steps = parseResponse(body, List.of("Kafka", "Docker"));

        assertThat(steps).hasSize(2);
        assertThat(steps.get(0).getTitle()).isEqualTo("Learn Kafka");
        assertThat(steps.get(1).getTitle()).isEqualTo("Learn Docker");
    }

    @Test
    @DisplayName("parseResponse : reply with backticks fences → dépouille et parse")
    void parses_response_wrapped_in_markdown_fences() throws Exception {
        String inner = """
            ```json
            {"steps":[{"title":"Foo","description":"","orderIndex":1,"estimatedDuration":"1w","resources":[]}]}
            ```
            """;

        List<RoadmapStep> steps = parseResponse(wrapAsGeminiResponse(inner), List.of("Foo"));

        assertThat(steps).hasSize(1);
        assertThat(steps.get(0).getTitle()).isEqualTo("Foo");
    }

    @Test
    @DisplayName("parseResponse : promptFeedback.blockReason → fallback")
    void blocked_prompt_falls_back() throws Exception {
        String blocked = """
            {"promptFeedback":{"blockReason":"SAFETY"}}
            """;

        List<RoadmapStep> steps = parseResponse(blocked, List.of("Python"));
        assertThat(steps).hasSize(1);
        assertThat(steps.get(0).getTitle()).isEqualTo("Learn Python");
    }

    @Test
    @DisplayName("parseResponse : payload Gemini avec error → fallback")
    void error_response_falls_back() throws Exception {
        String error = """
            {"error":{"code":429,"message":"quota exceeded"}}
            """;

        List<RoadmapStep> steps = parseResponse(error, List.of("Skill1", "Skill2"));
        assertThat(steps).hasSize(2);
    }

    @Test
    @DisplayName("parseResponse : JSON invalide → fallback (catch)")
    void invalid_json_falls_back() throws Exception {
        List<RoadmapStep> steps = parseResponse("not valid json", List.of("X"));
        assertThat(steps).hasSize(1);
        assertThat(steps.get(0).getTitle()).isEqualTo("Learn X");
    }

    @Test
    @DisplayName("fallbackSteps : 1 step par missing skill, status PENDING, ordre incrémental")
    void fallback_creates_one_step_per_skill() throws Exception {
        List<RoadmapStep> steps = fallbackSteps(List.of("Java", "Spring", "Kafka"));

        assertThat(steps).hasSize(3);
        assertThat(steps.get(0).getOrderIndex()).isEqualTo(1);
        assertThat(steps.get(2).getOrderIndex()).isEqualTo(3);
        assertThat(steps).allMatch(s -> s.getStatus() == StepStatus.PENDING);
        assertThat(steps).allMatch(s -> s.getResources().isEmpty());
    }

    @Test
    @DisplayName("fallbackSteps : liste vide → liste vide")
    void fallback_returns_empty_when_no_skills() throws Exception {
        List<RoadmapStep> steps = fallbackSteps(List.of());
        assertThat(steps).isEmpty();
    }
}

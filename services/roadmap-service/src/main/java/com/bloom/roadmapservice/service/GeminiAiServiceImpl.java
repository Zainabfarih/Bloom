package com.bloom.roadmapservice.service;

import com.bloom.roadmapservice.entity.Resource;
import com.bloom.roadmapservice.entity.RoadmapStep;
import com.bloom.roadmapservice.entity.StepStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class GeminiAiServiceImpl implements AiService {

    @Value("${ai.gemini.api-key}")
    private String geminiApiKey;

    @Value("${ai.gemini.model:gemini-2.5-flash}")
    private String model;

    @Value("${ai.gemini.max-output-tokens:4096}")
    private int maxOutputTokens;

    @Value("${ai.gemini.timeout-seconds:60}")
    private int timeoutSeconds;

    private final ObjectMapper mapper;

    // Thread-safe — même pattern que JobSkillExtractor
    private HttpClient httpClient;

    private static final String GEMINI_BASE_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/";

    @PostConstruct
    public void init() {
        httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        log.info("GeminiAiServiceImpl initialized — model={}", model);
    }

    @Override
    public List<RoadmapStep> generateLearningPath(String jobTitle, List<String> missingSkills) {
        try {
            // Payload — même structure que JobSkillExtractor
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("contents", List.of(
                    Map.of("parts", List.of(
                            Map.of("text", buildPrompt(jobTitle, missingSkills))
                    ))
            ));
            payload.put("generationConfig", Map.of(
                    "temperature",      0.2,
                    "maxOutputTokens",  maxOutputTokens,
                    "responseMimeType", "application/json"
            ));

            String url = GEMINI_BASE_URL + model + ":generateContent?key="
                    + URLEncoder.encode(geminiApiKey.trim(), StandardCharsets.UTF_8);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(payload)))
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() != 200) {
                log.error("Gemini HTTP {} — body: {}", response.statusCode(), response.body());
                return fallbackSteps(missingSkills);
            }

            return parseResponse(response.body(), missingSkills);

        } catch (Exception e) {
            log.error("Gemini API call failed: {}", e.getMessage());
            return fallbackSteps(missingSkills);
        }
    }

    private String buildPrompt(String jobTitle, List<String> missingSkills) {
        return """
            You are a technical learning path generator for IT students.
            Generate a structured learning roadmap for someone targeting the role: "%s".
            They are missing these skills: %s.

            Respond ONLY with raw JSON (no markdown, no explanation):
            {
              "steps": [
                {
                  "title": "Step title",
                  "description": "What to learn and why",
                  "orderIndex": 1,
                  "estimatedDuration": "2 weeks",
                  "resources": [
                    {"title": "Resource name", "url": "https://...", "type": "course|video|article|doc"}
                  ]
                }
              ]
            }

            Rules:
            - 4 to 8 steps maximum
            - Order steps logically (prerequisites first)
            - Include 2-3 resources per step
            - estimatedDuration as human-readable string
            """.formatted(jobTitle, missingSkills);
    }

    private List<RoadmapStep> parseResponse(String body, List<String> missingSkills) {
        try {
            JsonNode root = mapper.readTree(body);

            // Vérifier les erreurs Gemini
            JsonNode error = root.path("error");
            if (!error.isMissingNode()) {
                log.error("Gemini API error {}: {}",
                        error.path("code").asInt(),
                        error.path("message").asText());
                return fallbackSteps(missingSkills);
            }

            // Vérifier le blockReason
            JsonNode blockReason = root.path("promptFeedback").path("blockReason");
            if (!blockReason.isMissingNode() && !blockReason.asText("").isBlank()) {
                log.warn("Gemini blocked prompt: {}", blockReason.asText());
                return fallbackSteps(missingSkills);
            }

            // Extraire le texte généré
            String text = root.path("candidates")
                    .get(0)
                    .path("content")
                    .path("parts")
                    .get(0)
                    .path("text")
                    .asText("")
                    .trim();

            if (text.isBlank()) {
                log.warn("Gemini returned empty text");
                return fallbackSteps(missingSkills);
            }

            // Nettoyer les éventuels backticks (double sécurité malgré responseMimeType=json)
            String cleaned = text
                    .replaceAll("(?i)```json\\s*", "")
                    .replaceAll("```\\s*", "")
                    .trim();

            JsonNode stepsNode = mapper.readTree(cleaned).path("steps");

            if (!stepsNode.isArray() || stepsNode.isEmpty()) {
                log.warn("Gemini response has no 'steps' array");
                return fallbackSteps(missingSkills);
            }

            List<RoadmapStep> steps = new ArrayList<>();
            for (JsonNode node : stepsNode) {
                RoadmapStep step = new RoadmapStep();
                step.setTitle(node.path("title").asText("Untitled Step"));
                step.setDescription(node.path("description").asText(""));
                step.setOrderIndex(node.path("orderIndex").asInt(steps.size() + 1));
                step.setEstimatedDuration(node.path("estimatedDuration").asText("1-2 weeks"));
                step.setStatus(StepStatus.PENDING);
                step.setResources(new LinkedHashSet<>());

                JsonNode resourcesNode = node.path("resources");
                if (resourcesNode.isArray()) {
                    for (JsonNode r : resourcesNode) {
                        String title = r.path("title").asText("").trim();
                        String url   = r.path("url").asText("").trim();
                        if (title.isBlank() || url.isBlank()) continue; // skip invalid resources
                        Resource res = new Resource();
                        res.setTitle(title);
                        res.setUrl(url);
                        res.setType(r.path("type").asText("article"));
                        res.setStep(step);
                        step.getResources().add(res);
                    }
                }
                steps.add(step);
            }

            log.info("Gemini generated {} steps for roadmap", steps.size());
            return steps;

        } catch (Exception e) {
            log.error("Failed to parse Gemini response: {}", e.getMessage());
            return fallbackSteps(missingSkills);
        }
    }

    private List<RoadmapStep> fallbackSteps(List<String> missingSkills) {
        log.warn("Gemini fallback — generating 1 step per missing skill ({})", missingSkills.size());
        List<RoadmapStep> steps = new ArrayList<>();
        for (int i = 0; i < missingSkills.size(); i++) {
            String skill = missingSkills.get(i);
            RoadmapStep step = new RoadmapStep();
            step.setTitle("Learn " + skill);
            step.setDescription("Acquire foundational knowledge in " + skill);
            step.setOrderIndex(i + 1);
            step.setEstimatedDuration("1-2 weeks");
            step.setStatus(StepStatus.PENDING);
            step.setResources(new LinkedHashSet<>());
            steps.add(step);
        }
        return steps;
    }
}
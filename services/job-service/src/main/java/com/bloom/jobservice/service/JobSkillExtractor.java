package com.bloom.jobservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Extrait les compétences techniques d'une offre via Gemini, avec repli
 * sur un modèle Hugging Face (JobBERT) en cas d'échec.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class JobSkillExtractor {

    @Value("${skill.extractor.gemini.key}")
    private String geminiApiKey;

    @Value("${skill.extractor.hf.model-url}")
    private String hfModelUrl;

    @Value("${skill.extractor.gemini.base-url:https://generativelanguage.googleapis.com/v1beta/models/}")
    private String geminiBaseUrl;

    private static final String GEMINI_MODEL = "gemini-2.5-flash";
    private static final int    MAX_CHARS    = 3000;

    private final ObjectMapper mapper;
    private final HttpClient httpClient;

    public List<String> extract(String description) {
        if (description == null || description.isBlank()) return List.of();
        String text = description.length() > MAX_CHARS
                ? description.substring(0, MAX_CHARS)
                : description;
        return extractWithGemini(text);
    }

    private List<String> extractWithGemini(String text) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("contents", List.of(Map.of("parts", List.of(Map.of("text", buildGeminiPrompt(text))))));
            payload.put("generationConfig", Map.of(
                    "temperature",      0.0,
                    "maxOutputTokens",  2048,
                    "responseMimeType", "application/json"
            ));

            String url = geminiBaseUrl + GEMINI_MODEL + ":generateContent?key="
                    + URLEncoder.encode(geminiApiKey.trim(), StandardCharsets.UTF_8);

            HttpResponse<String> resp = httpClient.send(
                    HttpRequest.newBuilder()
                            .uri(URI.create(url))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(payload)))
                            .timeout(Duration.ofSeconds(30))
                            .build(),
                    HttpResponse.BodyHandlers.ofString()
            );

            if (resp.statusCode() != 200) {
                log.warn("Gemini HTTP {} — falling back to HF", resp.statusCode());
                return extractWithHuggingFace(text);
            }

            List<String> skills = parseGeminiSkills(resp.body());
            log.info("Gemini OK — {} skills extracted", skills.size());
            return skills;

        } catch (Exception e) {
            log.warn("Gemini failed ({}). Falling back to HF.", e.getMessage());
            return extractWithHuggingFace(text);
        }
    }

    private List<String> parseGeminiSkills(String body) throws Exception {
        JsonNode root = mapper.readTree(body);

        JsonNode error = root.path("error");
        if (!error.isMissingNode()) {
            log.warn("Gemini API error: {} — {}", error.path("code").asInt(), error.path("message").asText());
            return List.of();
        }

        JsonNode blockReason = root.path("promptFeedback").path("blockReason");
        if (!blockReason.isMissingNode() && !blockReason.asText("").isBlank()) {
            log.warn("Gemini blocked: {}", blockReason.asText());
            return List.of();
        }

        String text = root.path("candidates").get(0)
                .path("content").path("parts").get(0)
                .path("text").asText("").trim();

        if (text.isBlank()) {
            log.warn("Gemini empty response");
            return List.of();
        }

        return parseSkillsJson(text);
    }

    private String buildGeminiPrompt(String text) {
        return """
            Extract every technical skill from the job description below that belongs \
            in the "Technical Skills" section of an IT professional's CV or learning roadmap.

            A valid skill has a specific technical identity: a language, framework, library, \
            tool, platform, protocol, cloud service, database, standard, or specification \
            that can be learned, practiced, or certified.

            Do not extract: soft skills, personality traits, generic methodologies (Agile, Scrum…), \
            vague adjectives (scalable, distributed…), role labels (backend, engineer…), or IDEs/editors.

            Respond ONLY with raw JSON — no markdown, no explanation:
            {"skills": ["Skill1", "Skill2", ...]}

            Job description:
            """ + text + "\n\nJSON:";
    }

    private List<String> extractWithHuggingFace(String text) {
        try {
            HttpResponse<String> resp = httpClient.send(
                    HttpRequest.newBuilder()
                            .uri(URI.create(hfModelUrl.trim()))
                            .header("Content-Type", "application/json")
                            .header("User-Agent", "Spring-Boot-JobBERT-Client")
                            .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(Map.of("text", text))))
                            .timeout(Duration.ofSeconds(30))
                            .build(),
                    HttpResponse.BodyHandlers.ofString()
            );

            if (resp.statusCode() != 200) {
                log.error("HuggingFace KO (HTTP {}). Cannot extract skills.", resp.statusCode());
                return List.of();
            }

            List<String> skills = parseSkillsJson(resp.body());
            log.info("HuggingFace fallback OK — {} skills extracted", skills.size());
            return skills;

        } catch (Exception e) {
            log.error("Critical: both Gemini and HF failed.", e);
            return List.of();
        }
    }

    private List<String> parseSkillsJson(String jsonString) throws Exception {
        String cleaned = jsonString
                .replaceAll("(?i)```json\\s*", "")
                .replaceAll("```\\s*", "")
                .trim();

        JsonNode root = mapper.readTree(cleaned);
        JsonNode arr  = root.path("skills");

        if (!arr.isArray()) {
            log.warn("'skills' field missing or not an array in: {}", cleaned);
            return List.of();
        }

        List<String> skills = new ArrayList<>();
        for (JsonNode node : arr) {
            String s = node.asText("").trim();
            if (!s.isBlank() && s.length() >= 2 && s.length() <= 60) {
                skills.add(cleanSkill(s));
            }
        }

        return skills.stream()
                .filter(s -> !s.isBlank())
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.toList());
    }

    private String cleanSkill(String raw) {
        return raw.trim()
                .replaceAll("^[^a-zA-Z0-9]+", "")
                .replaceAll("[^a-zA-Z0-9.+#/_\\-]+$", "");
    }
}

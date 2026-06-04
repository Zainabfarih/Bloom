package com.bloom.cvservice.service;

import com.bloom.cvservice.dto.CvAnalysisIssue;
import com.bloom.cvservice.dto.CvAnalysisResponse;
import com.bloom.cvservice.exception.AiServiceException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Analyse ATS d'un CV à la volée via Gemini : score /100, points forts,
 * et problèmes (grammaire, structure, formatage, contenu).
 * <p>Aucun résultat n'est persisté — l'analyse est recalculée à chaque appel.</p>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CvAnalysisService {

    @Value("${cv.ai.gemini.key}")
    private String geminiApiKey;

    private static final String GEMINI_MODEL    = "gemini-2.5-flash";
    private static final String GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/";
    private static final int    MAX_CHARS       = 12000;

    private final ObjectMapper mapper;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public CvAnalysisResponse analyze(String cvText) {
        if (cvText == null || cvText.isBlank()) {
            throw new AiServiceException("Le CV ne contient aucun texte à analyser.");
        }
        String text = cvText.length() > MAX_CHARS ? cvText.substring(0, MAX_CHARS) : cvText;

        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("contents", List.of(Map.of("parts", List.of(Map.of("text", buildPrompt(text))))));
            payload.put("generationConfig", Map.of(
                    "temperature",      0.2,
                    "maxOutputTokens",  4096,
                    "responseMimeType", "application/json",
                    "thinkingConfig",   Map.of("thinkingBudget", 0) // évite la troncature du JSON
            ));

            String url = GEMINI_BASE_URL + GEMINI_MODEL + ":generateContent?key="
                    + URLEncoder.encode(geminiApiKey.trim(), StandardCharsets.UTF_8);

            HttpResponse<String> resp = httpClient.send(
                    HttpRequest.newBuilder()
                            .uri(URI.create(url))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(payload)))
                            .timeout(Duration.ofSeconds(40))
                            .build(),
                    HttpResponse.BodyHandlers.ofString()
            );

            if (resp.statusCode() != 200) {
                log.error("Gemini analyse HTTP {} — body={}", resp.statusCode(), resp.body());
                throw new AiServiceException("Gemini a renvoyé HTTP " + resp.statusCode());
            }

            return parseAnalysis(resp.body());

        } catch (AiServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("Échec de l'analyse ATS via Gemini", e);
            throw new AiServiceException("Analyse du CV indisponible : " + e.getMessage(), e);
        }
    }

    private String buildPrompt(String text) {
        return """
            You are an expert ATS (Applicant Tracking System) and professional CV reviewer.
            Analyze the CV below and return a strict, objective evaluation.

            Evaluate: grammar/spelling, structure (sections, ordering, completeness), \
            formatting/readability, and content quality (action verbs, quantified results, clarity).

            Respond ONLY with raw JSON — no markdown, no explanation — in exactly this shape:
            {
              "atsScore": <integer 0-100>,
              "summary": "<2-3 sentence overall assessment>",
              "strengths": ["<strength>", ...],
              "issues": [
                {
                  "type": "GRAMMAR | STRUCTURE | FORMATTING | CONTENT",
                  "severity": "LOW | MEDIUM | HIGH",
                  "message": "<what is wrong>",
                  "suggestion": "<concrete actionable fix>"
                }
              ]
            }

            CV:
            """ + text + "\n\nJSON:";
    }

    private CvAnalysisResponse parseAnalysis(String body) throws Exception {
        JsonNode root = mapper.readTree(body);

        JsonNode error = root.path("error");
        if (!error.isMissingNode()) {
            throw new AiServiceException("Gemini error: " + error.path("message").asText());
        }

        String text = root.path("candidates").get(0)
                .path("content").path("parts").get(0)
                .path("text").asText("").trim();

        if (text.isBlank()) {
            throw new AiServiceException("Réponse vide de Gemini lors de l'analyse.");
        }

        String cleaned = text
                .replaceAll("(?i)```json\\s*", "")
                .replaceAll("```\\s*", "")
                .trim();

        JsonNode json = mapper.readTree(cleaned);

        int score = clamp(json.path("atsScore").asInt(0));

        List<String> strengths = new ArrayList<>();
        for (JsonNode s : json.path("strengths")) {
            String v = s.asText("").trim();
            if (!v.isBlank()) strengths.add(v);
        }

        List<CvAnalysisIssue> issues = new ArrayList<>();
        for (JsonNode i : json.path("issues")) {
            issues.add(CvAnalysisIssue.builder()
                    .type(i.path("type").asText("CONTENT").trim())
                    .severity(i.path("severity").asText("LOW").trim())
                    .message(i.path("message").asText("").trim())
                    .suggestion(i.path("suggestion").asText("").trim())
                    .build());
        }

        return CvAnalysisResponse.builder()
                .atsScore(score)
                .summary(json.path("summary").asText("").trim())
                .strengths(strengths)
                .issues(issues)
                .build();
    }

    private int clamp(int score) {
        return Math.max(0, Math.min(100, score));
    }
}

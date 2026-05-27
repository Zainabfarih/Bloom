package com.bloom.jobservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.stream.Collectors;

/**
 * JobSkillExtractor — Extraction de compétences CV-ready.
 *
 * ARCHITECTURE :
 *   1. [PRINCIPAL]  Gemini API  — prompt concis et intelligent, JSON natif.
 *   2. [FALLBACK]   Hugging Face (JobBERT custom) — si Gemini KO.
 *   3. [COMMENTÉ]   Ollama (self-hosted) — prompt plus guidé adapté aux petits modèles.
 *
 * Pour basculer sur Ollama :
 *   - Décommentez les @Value ollama ci-dessous.
 *   - Décommentez la méthode extractWithOllama().
 *   - Dans extract(), remplacez extractWithGemini(text) par extractWithOllama(text).
 *   - Dans job-service.yaml, décommentez la section ollama:.
 *   Modèles recommandés : qwen2.5:3b (meilleur), llama3.2:3b, mistral:7b (précis/lent)
 */
@Service
@Slf4j
public class JobSkillExtractor {

    // ── Gemini (solution principale) ──────────────────────────────────────────

    @Value("${skill.extractor.gemini.key}")
    private String geminiApiKey;

    private static final String GEMINI_MODEL   = "gemini-2.5-flash";
    private static final String GEMINI_BASE_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/";

    // ── Hugging Face (fallback) ───────────────────────────────────────────────

    @Value("${skill.extractor.hf.model-url}")
    private String hfModelUrl;

    // ── Ollama (alternative self-hosted — COMMENTÉE) ──────────────────────────
    //
    // @Value("${skill.extractor.ollama.url}")
    // private String ollamaUrl;
    //
    // @Value("${skill.extractor.ollama.model}")
    // private String ollamaModel;
    //
    // @Value("${skill.extractor.ollama.timeout-seconds:120}")
    // private int ollamaTimeoutSeconds;

    // ── Paramètres ────────────────────────────────────────────────────────────

    private static final int MAX_CHARS = 3000;

    private final ObjectMapper mapper = new ObjectMapper();

    // ── Point d'entrée public ─────────────────────────────────────────────────

    public List<String> extract(String description) {
        if (description == null || description.isBlank()) return List.of();

        String text = description.length() > MAX_CHARS
                ? description.substring(0, MAX_CHARS)
                : description;

        return extractWithGemini(text);
    }

    // ── 1. Gemini API (solution principale) ───────────────────────────────────

    /**
     * Appelle Gemini avec responseMimeType="application/json" — JSON pur garanti.
     * Le prompt Gemini est volontairement concis : Gemini comprend le contexte
     * sans sur-explication et produit de meilleurs résultats avec moins de bruit.
     */
    private List<String> extractWithGemini(String text) {
        try {
            Map<String, Object> textPart  = Map.of("text", buildGeminiPrompt(text));
            Map<String, Object> content   = Map.of("parts", List.of(textPart));
            Map<String, Object> genConfig = Map.of(
                    "temperature",      0.0,
                    "maxOutputTokens",  1024,  // ← suffisant pour ~50 skills en JSON
                    "responseMimeType", "application/json"
            );

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("contents",         List.of(content));
            payload.put("generationConfig", genConfig);

            String body = mapper.writeValueAsString(payload);
            String url  = GEMINI_BASE_URL + GEMINI_MODEL + ":generateContent?key="
                    + URLEncoder.encode(geminiApiKey.trim(), StandardCharsets.UTF_8);

            HttpClient http = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .timeout(Duration.ofSeconds(30))
                    .build();

            log.debug("Gemini → model={}", GEMINI_MODEL);
            HttpResponse<String> resp = http.send(request, HttpResponse.BodyHandlers.ofString());

            if (resp.statusCode() != 200) {
                log.warn("Gemini HTTP {} — body={}. Bascule HF.", resp.statusCode(), resp.body());
                return extractWithHuggingFace(text);
            }

            List<String> skills = parseGeminiSkills(resp.body());
            log.info("Gemini OK — {} skills extraits", skills.size());
            return skills;

        } catch (Exception e) {
            log.warn("Gemini KO ({}). Bascule HF.", e.getMessage());
            return extractWithHuggingFace(text);
        }
    }

    private List<String> parseGeminiSkills(String body) throws Exception {
        JsonNode root;
        try {
            root = mapper.readTree(body);
        } catch (Exception e) {
            log.warn("Gemini — JSON mal formé (réponse probablement tronquée). Longueur body={}", body.length());
            return List.of();
        }

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
            log.warn("Gemini response vide — raw body: {}", body);
            return List.of();
        }

        return parseSkillsJson(text);
    }

    // ── Prompt Gemini — concis et direct ─────────────────────────────────────

    /**
     * Prompt conçu pour Gemini 2.0 Flash.
     *
     * Gemini est un LLM avancé qui comprend le contexte métier IT sans avoir besoin
     * d'une liste exhaustive de catégories. Un prompt court et précis donne de
     * meilleurs résultats qu'un prompt verbeux — moins de hallucinations, moins
     * de bruit, meilleure extraction des skills implicitement techniques.
     *
     * Le travail d'identification est entièrement délégué au modèle.
     */
    private String buildGeminiPrompt(String text) {
        return """
            Extract every technical skill from the job description below that belongs \
            in the "Technical Skills" section of an IT professional's CV or learning roadmap.

            A valid skill has a specific technical identity: a language, framework, library, \
            tool, platform, protocol, cloud service, database, standard, or specification \
            that can be learned, practiced, or certified — applicable to any IT profile \
            (dev, devops, data, cloud, security, QA, network, etc.).

            Do not extract: soft skills, personality traits, generic methodologies (Agile, Scrum…), \
            vague adjectives (scalable, distributed…), role labels (backend, engineer…), \
            or IDEs/editors.

            Respond ONLY with raw JSON — no markdown, no explanation:
            {"skills": ["Skill1", "Skill2", ...]}

            Job description:
            """ + text + "\n\nJSON:";
    }

    // ── 2. Fallback Hugging Face ──────────────────────────────────────────────

    private List<String> extractWithHuggingFace(String text) {
        try {
            String body = mapper.writeValueAsString(Map.of("text", text));

            HttpClient http = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(hfModelUrl.trim()))
                    .header("Content-Type", "application/json")
                    .header("User-Agent", "Spring-Boot-JobBERT-Client")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .timeout(Duration.ofSeconds(30))
                    .build();

            HttpResponse<String> resp = http.send(request, HttpResponse.BodyHandlers.ofString());

            if (resp.statusCode() != 200) {
                log.error("Hugging Face KO (HTTP {}). Extraction impossible.", resp.statusCode());
                return List.of();
            }

            List<String> skills = parseSkillsJson(resp.body());
            log.info("HuggingFace fallback OK — {} skills extraits", skills.size());
            return skills;

        } catch (Exception e) {
            log.error("Échec critique pipeline extraction (Gemini + HF KO).", e);
            return List.of();
        }
    }

    // ── 3. Ollama (self-hosted — COMMENTÉE) ───────────────────────────────────

    /*
     * Le prompt Ollama est plus verbeux que le prompt Gemini intentionnellement :
     * les petits modèles locaux (3B-7B) ont besoin de plus de structure et
     * d'exemples d'exclusion explicites pour éviter les hallucinations et le bruit.
     *
     * private List<String> extractWithOllama(String text) {
     *     try {
     *         Map<String, Object> options = new LinkedHashMap<>();
     *         options.put("temperature", 0.0);
     *         options.put("num_predict", 512);
     *         options.put("num_ctx",     2048);  // évite la troncature silencieuse
     *
     *         Map<String, Object> payload = new LinkedHashMap<>();
     *         payload.put("model",   ollamaModel.trim());
     *         payload.put("prompt",  buildOllamaPrompt(text));
     *         payload.put("stream",  false);
     *         payload.put("format",  "json");    // JSON natif, sans balises Markdown
     *         payload.put("options", options);
     *
     *         String body    = mapper.writeValueAsString(payload);
     *         String baseUrl = ollamaUrl.trim();
     *         if (!baseUrl.endsWith("/")) baseUrl += "/";
     *
     *         HttpClient http = HttpClient.newBuilder()
     *                 .connectTimeout(Duration.ofSeconds(10))
     *                 .build();
     *
     *         HttpRequest request = HttpRequest.newBuilder()
     *                 .uri(URI.create(baseUrl + "api/generate"))
     *                 .header("Content-Type", "application/json")
     *                 .POST(HttpRequest.BodyPublishers.ofString(body))
     *                 .timeout(Duration.ofSeconds(ollamaTimeoutSeconds))
     *                 .build();
     *
     *         log.debug("Ollama → model={} num_ctx=2048 timeout={}s", ollamaModel, ollamaTimeoutSeconds);
     *         HttpResponse<String> resp = http.send(request, HttpResponse.BodyHandlers.ofString());
     *
     *         if (resp.statusCode() != 200) {
     *             log.warn("Ollama HTTP {}. Bascule HF.", resp.statusCode());
     *             return extractWithHuggingFace(text);
     *         }
     *
     *         JsonNode root = mapper.readTree(resp.body());
     *         String responseText = root.path("response").asText("").trim();
     *         if (responseText.isBlank()) {
     *             log.warn("Ollama response vide. Bascule HF.");
     *             return extractWithHuggingFace(text);
     *         }
     *
     *         List<String> skills = parseSkillsJson(responseText);
     *         log.info("Ollama OK — {} skills extraits", skills.size());
     *         return skills;
     *
     *     } catch (Exception e) {
     *         log.warn("Ollama KO ({}). Bascule HF.", e.getMessage());
     *         return extractWithHuggingFace(text);
     *     }
     * }
     */

    // ── Prompt Ollama — structuré et guidé ────────────────────────────────────

    /*
     * Prompt conçu pour les petits modèles locaux (3B-7B paramètres).
     *
     * Contrairement à Gemini, les modèles locaux ont tendance à :
     *   - inclure des soft skills ou des rôles si on ne l'interdit pas explicitement,
     *   - mal formater le JSON sans exemples de structure,
     *   - halluciner des skills absents du texte si le prompt est trop ouvert.
     *
     * Stratégie : catégories INCLUDE + exemples EXCLUDE + structure JSON explicite.
     * num_ctx=2048 est requis pour que le modèle voie le prompt complet.
     *
     * private String buildOllamaPrompt(String text) {
     *     return """
     *         You are a technical recruiter. Read the job description and extract every \
     *         technical skill that belongs in the "Technical Skills" section of an IT CV.
     *
     *         INCLUDE any specific named technology with technical identity:
     *         programming languages, frameworks, libraries, databases, cloud services,
     *         DevOps tools, CI/CD tools, containers, messaging systems, protocols,
     *         APIs, testing frameworks, monitoring tools, security tools, BPM engines,
     *         build tools, data engineering tools, version control systems.
     *
     *         DO NOT include:
     *         - Soft skills: Communication, Leadership, Teamwork, Problem solving
     *         - Methodologies: Agile, Scrum, Kanban, SAFe, Lean
     *         - Role labels: Backend, Frontend, Developer, Engineer, Architect
     *         - Vague adjectives: Scalable, Distributed, Resilient, Serverless, Event-driven
     *         - IDEs or editors: IntelliJ, VS Code, Eclipse
     *
     *         RULES:
     *         - Extract ONLY skills explicitly present in the text.
     *         - Keep official names and casing.
     *         - Include version if specified (e.g. Java 17, Spring Boot 3).
     *         - No duplicates.
     *
     *         Return ONLY this JSON structure, nothing else:
     *         {"skills": ["Skill1", "Skill2", "Skill3"]}
     *
     *         Job description:
     *         """ + text + """
     *
     *         JSON:
     *         """;
     * }
     */

    // ── Parsing JSON partagé (Gemini + Ollama + HF) ───────────────────────────

    private List<String> parseSkillsJson(String jsonString) throws Exception {
        String cleaned = jsonString
                .replaceAll("(?i)```json\\s*", "")
                .replaceAll("```\\s*", "")
                .trim();

        JsonNode root = mapper.readTree(cleaned);
        JsonNode arr  = root.path("skills");

        if (!arr.isArray()) {
            log.warn("Champ 'skills' absent ou non-array dans : {}", cleaned);
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
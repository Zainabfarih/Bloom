package com.bloom.cvservice.controller;

import com.bloom.cvservice.dto.CvAnalysisResponse;
import com.bloom.cvservice.dto.CvResponse;
import com.bloom.cvservice.dto.ManualCvRequest;
import com.bloom.cvservice.dto.SkillsDTO;
import com.bloom.cvservice.service.CvService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/cv")
@RequiredArgsConstructor
@Tag(name = "CV", description = "Upload de CV (PDF), saisie manuelle, extraction de skills et analyse ATS")
public class CvController {

    private final CvService cvService;

    // ─── Upload PDF ──────────────────────────────────────────────────────────

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload d'un CV PDF — texte extrait + skills détectés par IA")
    public ResponseEntity<CvResponse> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "title", required = false) String title,
            Authentication auth) {

        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(cvService.uploadCv(userId, file, title));
    }

    // ─── Saisie manuelle ───────────────────────────────────────────────────────

    @PostMapping("/manual")
    @Operation(summary = "Création d'un CV par saisie manuelle des sections")
    public ResponseEntity<CvResponse> createManual(
            @Valid @RequestBody ManualCvRequest request,
            Authentication auth) {

        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(cvService.createManualCv(userId, request));
    }

    // ─── Lectures ──────────────────────────────────────────────────────────────

    @GetMapping("/me")
    @Operation(summary = "Récupère le CV actif de l'étudiant connecté")
    public ResponseEntity<CvResponse> getActive(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(cvService.getActiveCv(userId));
    }

    @GetMapping("/me/all")
    @Operation(summary = "Liste tous les CV de l'étudiant connecté")
    public ResponseEntity<List<CvResponse>> getMyCvs(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(cvService.getMyCvs(userId));
    }

    // ─── Contrat consommé par job-service (Feign) ──────────────────────────────

    @GetMapping("/users/{userId}/skills")
    @Operation(summary = "Skills du CV actif d'un étudiant — consommé par job-service")
    public ResponseEntity<SkillsDTO> getUserSkills(@PathVariable Long userId) {
        return ResponseEntity.ok(cvService.getUserSkills(userId));
    }

    @GetMapping("/{cvUuid}/skills")
    @Operation(summary = "Skills d'un CV précis par UUID — consommé par job-service")
    public ResponseEntity<SkillsDTO> getCvSkills(@PathVariable UUID cvUuid) {
        return ResponseEntity.ok(cvService.getCvSkills(cvUuid));
    }

    // ─── Analyse ATS (à la volée, non persistée) ────────────────────────────────

    @GetMapping("/{cvUuid}/analysis")
    @Operation(summary = "Analyse ATS du CV — score, grammaire, structure, contenu (calcul à la volée)")
    public ResponseEntity<CvAnalysisResponse> analyze(
            @PathVariable UUID cvUuid,
            Authentication auth) {

        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(cvService.analyzeCv(userId, cvUuid));
    }

    // ─── Suppression ─────────────────────────────────────────────────────────────

    @DeleteMapping("/{cvUuid}")
    @Operation(summary = "Supprime un CV de l'étudiant connecté")
    public ResponseEntity<Void> delete(
            @PathVariable UUID cvUuid,
            Authentication auth) {

        Long userId = (Long) auth.getPrincipal();
        cvService.deleteCv(userId, cvUuid);
        return ResponseEntity.noContent().build();
    }
}

package com.bloom.jobservice.controller;

import com.bloom.jobservice.dto.JobDetailResponse;
import com.bloom.jobservice.dto.JobSearchResponse;
import com.bloom.jobservice.dto.SaveJobRequest;
import com.bloom.jobservice.dto.SavedJobResponse;
import com.bloom.jobservice.service.JobService;
import com.bloom.jobservice.service.SavedJobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/job")
@RequiredArgsConstructor
@Tag(name = "Jobs", description = "Search jobs and manage favourites")
public class JobController {

    private final JobService jobService;
    private final SavedJobService savedJobService;

    // ─── Search ──────────────────────────────────────────────────────────────

    @GetMapping("/search")
    @Operation(summary = "Search jobs — résultats SerpAPI, cached 24h, sans skills (lazy extraction)")
    public ResponseEntity<JobSearchResponse> search(
            @RequestParam String query,
            @RequestParam(required = false) String location) {

        return ResponseEntity.ok(jobService.searchJobs(query, location));
    }

    // ─── Job details ──────────────────────────────────────────────────────────────

    @GetMapping("/{jobId}")
    @Operation(summary = "Détail d'un job + skills extraits — nécessite un search préalable")
    public ResponseEntity<JobDetailResponse> getJobDetail(
            @PathVariable String jobId) {

        return ResponseEntity.ok(jobService.getJobDetail(jobId));
    }

    // ─── Favourites ──────────────────────────────────────────────────────────

    @PostMapping("/saved")
    @Operation(summary = "Save a job — skill matching vs CV, score calculé server-side")
    public ResponseEntity<SavedJobResponse> save(
            @Valid @RequestBody SaveJobRequest request,
            Authentication auth,
            @RequestHeader("Authorization") String bearerToken) {

        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(savedJobService.saveJob(userId, request, bearerToken));
    }

    @GetMapping("/saved")
    @Operation(summary = "Get all my saved jobs ordered by compatibility score DESC")
    public ResponseEntity<List<SavedJobResponse>> getMySaved(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(savedJobService.getSavedJobs(userId));
    }

    @GetMapping("/saved/{uuid}")
    @Operation(summary = "Get one saved job by UUID — consumed by roadmap-service")
    public ResponseEntity<SavedJobResponse> getSavedJobByUuid(
            @PathVariable UUID uuid,
            Authentication auth) {

        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(savedJobService.getByUuid(userId, uuid));
    }

    @DeleteMapping("/saved/{jobExternalId}")
    @Operation(summary = "Remove a job from favourites")
    public ResponseEntity<Void> removeSavedJob(
            @PathVariable String jobExternalId,
            Authentication auth) {

        Long userId = (Long) auth.getPrincipal();
        savedJobService.removeSavedJob(userId, jobExternalId);
        return ResponseEntity.noContent().build();
    }

    // ─── Admin ───────────────────────────────────────────────────────────────

    @DeleteMapping("/admin/cache")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Force evict Redis cache (search + skills) pour une query donnée")
    public ResponseEntity<Void> evictCache(
            @RequestParam String query,
            @RequestParam(required = false) String location) {

        jobService.evictCache(query, location);
        return ResponseEntity.noContent().build();
    }
}

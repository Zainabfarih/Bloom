package com.bloom.jobservice.controller;

import com.bloom.jobservice.dto.JobSearchResponse;
import com.bloom.jobservice.dto.SaveJobRequest;
import com.bloom.jobservice.dto.SavedJobResponse;
import com.bloom.jobservice.service.JobSearchService;
import com.bloom.jobservice.service.UserJobService;
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
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
@Tag(name = "Jobs", description = "Search jobs and manage favourites")
public class JobController {

    private final JobSearchService jobSearchService;
    private final UserJobService  userJobService;

    // ─── Search ──────────────────────────────────────────────

    @GetMapping("/search")
    @Operation(summary = "Search jobs via JobsAPI — cached 24h in Redis")
    public ResponseEntity<JobSearchResponse> search(
            @RequestParam String query,
            @RequestParam(required = false) String location) {

        return ResponseEntity.ok(
                jobSearchService.searchJobs(query, location));
    }

    // ─── Favourites ───────────────────────────────────────────

    @PostMapping("/saved")
    @Operation(summary = "Save a job as favourite — triggers skill matching")
    public ResponseEntity<SavedJobResponse> save(
            @Valid @RequestBody SaveJobRequest request,
            Authentication auth,
            @RequestHeader("Authorization") String bearerToken) {

        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(userJobService.saveJob(userId, request, bearerToken));
    }

    @GetMapping("/saved")
    @Operation(summary = "Get all my saved jobs ordered by compatibility score")
    public ResponseEntity<List<SavedJobResponse>> getMySaved(
            Authentication auth) {

        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(
                userJobService.getUserJobs(userId));
    }

    @GetMapping("/saved/{uuid}")
    @Operation(summary = "Get one saved job by UUID — consumed by roadmap-service")
    public ResponseEntity<SavedJobResponse> getOne(
            @PathVariable UUID uuid,
            Authentication auth) {

        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(
                userJobService.getByUuid(userId, uuid));
    }

    @DeleteMapping("/saved/{jobExternalId}")
    @Operation(summary = "Remove a job from favourites")
    public ResponseEntity<Void> remove(
            @PathVariable String jobExternalId,
            Authentication auth) {

        Long userId = (Long) auth.getPrincipal();
        userJobService.removeSavedJob(userId, jobExternalId);
        return ResponseEntity.noContent().build();
    }

    // ─── Admin ───────────────────────────────────────────────

    @DeleteMapping("/admin/cache")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Force evict Redis cache for a specific query")
    public ResponseEntity<Void> evictCache(
            @RequestParam String query,
            @RequestParam(required = false) String location) {

        jobSearchService.evictCache(query, location);
        return ResponseEntity.noContent().build();
    }
}
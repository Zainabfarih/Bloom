package com.bloom.roadmapservice.controller;

import com.bloom.roadmapservice.dto.*;
import com.bloom.roadmapservice.service.RoadmapService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/roadmap")
@RequiredArgsConstructor
@Tag(name = "Roadmap", description = "AI-generated learning roadmaps")
public class RoadmapController {

    private final RoadmapService roadmapService;

    @PostMapping("/generate")
    @Operation(summary = "Generate a roadmap for the authenticated user targeting a saved job")
    public ResponseEntity<RoadmapResponse> generateRoadmap(
            Authentication auth,
            @Valid @RequestBody RoadmapGenerationRequest req) {

        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED).body(roadmapService.generateRoadmap(userId, req));
    }

    @GetMapping
    @Operation(summary = "Get all roadmaps for the authenticated user")
    public ResponseEntity<List<RoadmapResponse>> getMyRoadmaps(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(roadmapService.getUserRoadmaps(userId));
    }

    @PatchMapping("/steps/{stepId}/status")
    @Operation(summary = "Update a step status (PENDING → IN_PROGRESS → COMPLETED)")
    public ResponseEntity<RoadmapResponse> updateStepStatus(
            Authentication auth,
            @PathVariable Long stepId,
            @Valid @RequestBody StepStatusUpdateDTO dto) {

        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(roadmapService.updateStepStatus(userId, stepId, dto));
    }
}
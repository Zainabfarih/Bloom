package com.bloom.roadmapservice.dto;

import jakarta.validation.constraints.NotNull;

public record RoadmapGenerationRequest(@NotNull Long targetJobId) {}
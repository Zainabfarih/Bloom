package com.bloom.roadmapservice.dto;

import java.util.List;

public record RoadmapResponse(
        Long id,
        Long targetJobId,
        String targetJobTitle,
        Integer progressPercentage,
        List<StepDTO> steps
) {}
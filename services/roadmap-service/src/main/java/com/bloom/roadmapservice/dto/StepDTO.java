package com.bloom.roadmapservice.dto;

import com.bloom.roadmapservice.entity.StepStatus;
import java.util.List;

public record StepDTO(
        Long id,
        String title,
        String description,
        Integer orderIndex,
        StepStatus status,
        String estimatedDuration,
        List<ResourceDTO> resources
) {}
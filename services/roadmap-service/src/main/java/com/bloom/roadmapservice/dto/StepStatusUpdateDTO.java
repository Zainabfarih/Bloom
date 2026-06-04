package com.bloom.roadmapservice.dto;

import com.bloom.roadmapservice.entity.StepStatus;
import jakarta.validation.constraints.NotNull;

public record StepStatusUpdateDTO(@NotNull StepStatus newStatus) {}
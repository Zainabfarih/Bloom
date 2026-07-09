package com.bloom.jobservice.dto;

import java.util.List;

public record SkillGapResponse(Long userId, Long jobId, String jobTitle, List<String> missingSkills) {}
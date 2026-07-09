package com.bloom.roadmapservice.service;

import com.bloom.roadmapservice.entity.RoadmapStep;
import java.util.List;

public interface AiService {
    List<RoadmapStep> generateLearningPath(String jobTitle, List<String> missingSkills);
}
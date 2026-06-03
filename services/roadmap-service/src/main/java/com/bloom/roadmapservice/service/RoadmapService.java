package com.bloom.roadmapservice.service;

import com.bloom.roadmapservice.dto.*;
import com.bloom.roadmapservice.entity.Roadmap;
import com.bloom.roadmapservice.entity.RoadmapStep;
import com.bloom.roadmapservice.entity.StepStatus;
import com.bloom.roadmapservice.exception.RoadmapNotFoundException;
import com.bloom.roadmapservice.exception.StepNotFoundException;
import com.bloom.roadmapservice.external.JobServiceClient;
import com.bloom.roadmapservice.mapper.RoadmapMapper;
import com.bloom.roadmapservice.repository.RoadmapRepository;
import com.bloom.roadmapservice.repository.RoadmapStepRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class RoadmapService {

    private final RoadmapRepository    roadmapRepo;
    private final RoadmapStepRepository stepRepo;
    private final JobServiceClient     jobClient;
    private final AiService            aiService;
    private final RoadmapMapper        roadmapMapper;

    @Value("${internal.security.gateway-secret}")
    private String gatewaySecret;

    public RoadmapResponse generateRoadmap(Long userId, RoadmapGenerationRequest req) {
        // Idempotent — retourne l'existant sans rappeler l'IA
        return roadmapRepo.findByUserIdAndTargetJobId(userId, req.targetJobId())
                .map(existing -> {
                    log.info("Roadmap already exists — userId={}, jobId={}", userId, req.targetJobId());
                    return roadmapMapper.toResponse(existing);
                })
                .orElseGet(() -> createRoadmap(userId, req));
    }

    private RoadmapResponse createRoadmap(Long userId, RoadmapGenerationRequest req) {
        SkillGapResponse gap = jobClient.getJobSkillGap(
                userId,
                req.targetJobId(),
                String.valueOf(userId),   // X-User-Id header
                gatewaySecret             // X-Gateway-Secret header
        );

        String jobTitle = (gap.jobTitle() != null && !gap.jobTitle().isBlank())
                ? gap.jobTitle()
                : "Target Job #" + req.targetJobId();

        List<String> missingSkills = gap.missingSkills() != null ? gap.missingSkills() : List.of();

        if (missingSkills.isEmpty()) {
            log.warn("No missing skills found for userId={}, jobId={} — generating generic roadmap",
                    userId, req.targetJobId());
        }

        List<RoadmapStep> steps = aiService.generateLearningPath(jobTitle, missingSkills);

        Roadmap roadmap = Roadmap.builder()
                .userId(userId)
                .targetJobId(req.targetJobId())
                .targetJobTitle(jobTitle)
                .progressPercentage(0)
                .build();

        steps.forEach(step -> {
            step.setRoadmap(roadmap);
            step.getResources().forEach(r -> r.setStep(step));
        });
        roadmap.setSteps(steps);

        Roadmap saved = roadmapRepo.save(roadmap);
        log.info("Roadmap created — userId={}, jobTitle='{}', steps={}", userId, jobTitle, steps.size());
        return roadmapMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<RoadmapResponse> getUserRoadmaps(Long userId) {
        return roadmapMapper.toResponseList(roadmapRepo.findByUserId(userId));
    }

    public RoadmapResponse updateStepStatus(Long userId, Long stepId, StepStatusUpdateDTO dto) {
        RoadmapStep step = stepRepo.findByIdAndRoadmapUserId(stepId, userId)
                .orElseThrow(() -> new StepNotFoundException(stepId));

        step.setStatus(dto.newStatus());

        Roadmap roadmap = step.getRoadmap();
        recalculateProgress(roadmap);
        roadmapRepo.save(roadmap);

        // Reload avec JOIN FETCH pour que le mapper ait steps à jour sans lazy loading
        return roadmapRepo.findByIdWithSteps(roadmap.getId())
                .map(roadmapMapper::toResponse)
                .orElseThrow(() -> new RoadmapNotFoundException(userId, roadmap.getTargetJobId()));
    }

    private void recalculateProgress(Roadmap roadmap) {
        List<RoadmapStep> steps = roadmap.getSteps();
        if (steps.isEmpty()) {
            roadmap.setProgressPercentage(0);
            return;
        }
        long completed = steps.stream()
                .filter(s -> s.getStatus() == StepStatus.COMPLETED || s.getStatus() == StepStatus.ACCEPTED)
                .count();
        roadmap.setProgressPercentage((int) Math.round((double) completed / steps.size() * 100));
    }
}
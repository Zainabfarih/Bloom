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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoadmapServiceTest {

    @Mock private RoadmapRepository roadmapRepo;
    @Mock private RoadmapStepRepository stepRepo;
    @Mock private JobServiceClient jobClient;
    @Mock private AiService aiService;
    @Mock private RoadmapMapper roadmapMapper;

    @InjectMocks
    private RoadmapService roadmapService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(roadmapService, "gatewaySecret", "test-gateway-secret");
    }

    private RoadmapStep newStep(int idx, StepStatus status) {
        RoadmapStep s = new RoadmapStep();
        s.setId((long) idx);
        s.setOrderIndex(idx);
        s.setTitle("Step " + idx);
        s.setStatus(status);
        s.setResources(new java.util.LinkedHashSet<>());
        return s;
    }

    private Roadmap newRoadmap(Long id, Long userId, Long jobId, String title, List<RoadmapStep> steps) {
        Roadmap r = Roadmap.builder()
                .id(id).userId(userId).targetJobId(jobId).targetJobTitle(title)
                .progressPercentage(0).steps(new ArrayList<>(steps))
                .build();
        steps.forEach(s -> s.setRoadmap(r));
        return r;
    }

    private RoadmapResponse responseFor(Roadmap r) {
        return new RoadmapResponse(
                r.getId(), r.getTargetJobId(), r.getTargetJobTitle(),
                r.getProgressPercentage(), List.of());
    }

    // ─── generateRoadmap ───────────────────────────────────────────────

    @Test
    @DisplayName("generateRoadmap : roadmap déjà existant → idempotent (pas d'appel IA)")
    void generate_returns_existing_roadmap_without_ai_call() {
        Roadmap existing = newRoadmap(10L, 1L, 1001L, "Frontend Dev", List.of());
        RoadmapGenerationRequest req = new RoadmapGenerationRequest(1001L);

        when(roadmapRepo.findByUserIdAndTargetJobId(1L, 1001L)).thenReturn(Optional.of(existing));
        when(roadmapMapper.toResponse(existing)).thenReturn(responseFor(existing));

        RoadmapResponse result = roadmapService.generateRoadmap(1L, req);

        assertThat(result.id()).isEqualTo(10L);
        verifyNoInteractions(jobClient, aiService);
    }

    @Test
    @DisplayName("generateRoadmap : nouveau roadmap → appelle job-service puis AI")
    void generate_creates_roadmap_when_not_existing() {
        RoadmapGenerationRequest req = new RoadmapGenerationRequest(1001L);
        when(roadmapRepo.findByUserIdAndTargetJobId(1L, 1001L)).thenReturn(Optional.empty());

        SkillGapResponse gap = new SkillGapResponse(
                1L, 1001L, "Frontend Developer", List.of("React", "TypeScript"));
        when(jobClient.getJobSkillGap(1L, 1001L, "1", "test-gateway-secret")).thenReturn(gap);

        List<RoadmapStep> aiSteps = List.of(newStep(1, StepStatus.PENDING), newStep(2, StepStatus.PENDING));
        when(aiService.generateLearningPath("Frontend Developer", List.of("React", "TypeScript")))
                .thenReturn(aiSteps);

        when(roadmapRepo.save(any(Roadmap.class))).thenAnswer(inv -> {
            Roadmap saved = inv.getArgument(0);
            saved.setId(42L);
            return saved;
        });
        when(roadmapMapper.toResponse(any(Roadmap.class))).thenAnswer(inv -> {
            Roadmap r = inv.getArgument(0);
            return new RoadmapResponse(r.getId(), r.getTargetJobId(), r.getTargetJobTitle(),
                    r.getProgressPercentage(), List.of());
        });

        RoadmapResponse result = roadmapService.generateRoadmap(1L, req);

        assertThat(result.id()).isEqualTo(42L);
        assertThat(result.targetJobTitle()).isEqualTo("Frontend Developer");
        verify(jobClient).getJobSkillGap(1L, 1001L, "1", "test-gateway-secret");
        verify(aiService).generateLearningPath(any(), anyList());
    }

    @Test
    @DisplayName("generateRoadmap : jobTitle null/blank → fallback 'Target Job #id'")
    void generate_uses_fallback_title_when_blank() {
        RoadmapGenerationRequest req = new RoadmapGenerationRequest(2042L);
        when(roadmapRepo.findByUserIdAndTargetJobId(1L, 2042L)).thenReturn(Optional.empty());

        SkillGapResponse gap = new SkillGapResponse(1L, 2042L, "  ", List.of("Python"));
        when(jobClient.getJobSkillGap(1L, 2042L, "1", "test-gateway-secret")).thenReturn(gap);
        when(aiService.generateLearningPath(eq("Target Job #2042"), anyList()))
                .thenReturn(List.of(newStep(1, StepStatus.PENDING)));
        when(roadmapRepo.save(any(Roadmap.class))).thenAnswer(inv -> inv.getArgument(0));
        when(roadmapMapper.toResponse(any(Roadmap.class))).thenAnswer(inv ->
                new RoadmapResponse(null, 2042L, "Target Job #2042", 0, List.of()));

        RoadmapResponse result = roadmapService.generateRoadmap(1L, req);
        assertThat(result.targetJobTitle()).isEqualTo("Target Job #2042");
    }

    @Test
    @DisplayName("generateRoadmap : missingSkills null → traité comme liste vide")
    void generate_handles_null_missing_skills() {
        RoadmapGenerationRequest req = new RoadmapGenerationRequest(3001L);
        when(roadmapRepo.findByUserIdAndTargetJobId(1L, 3001L)).thenReturn(Optional.empty());
        when(jobClient.getJobSkillGap(any(), any(), any(), any()))
                .thenReturn(new SkillGapResponse(1L, 3001L, "Devops", null));
        when(aiService.generateLearningPath(any(), anyList()))
                .thenReturn(List.of(newStep(1, StepStatus.PENDING)));
        when(roadmapRepo.save(any(Roadmap.class))).thenAnswer(inv -> inv.getArgument(0));
        when(roadmapMapper.toResponse(any(Roadmap.class)))
                .thenReturn(new RoadmapResponse(1L, 3001L, "Devops", 0, List.of()));

        roadmapService.generateRoadmap(1L, req);

        verify(aiService).generateLearningPath("Devops", List.of());
    }

    // ─── getUserRoadmaps ───────────────────────────────────────────────

    @Test
    @DisplayName("getUserRoadmaps : délègue à la repo et au mapper")
    void getUserRoadmaps_delegates() {
        Roadmap r = newRoadmap(1L, 1L, 1001L, "Frontend", List.of());
        when(roadmapRepo.findByUserId(1L)).thenReturn(List.of(r));
        when(roadmapMapper.toResponseList(List.of(r)))
                .thenReturn(List.of(responseFor(r)));

        List<RoadmapResponse> result = roadmapService.getUserRoadmaps(1L);
        assertThat(result).hasSize(1);
    }

    // ─── updateStepStatus ──────────────────────────────────────────────

    @Test
    @DisplayName("updateStepStatus : COMPLETED sur 1/2 steps → progress=50")
    void updateStepStatus_recalculates_progress() {
        RoadmapStep s1 = newStep(1, StepStatus.PENDING);
        RoadmapStep s2 = newStep(2, StepStatus.PENDING);
        Roadmap roadmap = newRoadmap(10L, 1L, 1001L, "Frontend", List.of(s1, s2));

        when(stepRepo.findByIdAndRoadmapUserId(1L, 1L)).thenReturn(Optional.of(s1));
        when(roadmapRepo.findByIdWithSteps(10L)).thenReturn(Optional.of(roadmap));
        when(roadmapMapper.toResponse(roadmap)).thenAnswer(inv ->
                new RoadmapResponse(10L, 1001L, "Frontend",
                        ((Roadmap) inv.getArgument(0)).getProgressPercentage(), List.of()));

        StepStatusUpdateDTO dto = new StepStatusUpdateDTO(StepStatus.COMPLETED);
        RoadmapResponse result = roadmapService.updateStepStatus(1L, 1L, dto);

        assertThat(roadmap.getProgressPercentage()).isEqualTo(50);
        assertThat(s1.getStatus()).isEqualTo(StepStatus.COMPLETED);
        assertThat(result.progressPercentage()).isEqualTo(50);
        verify(roadmapRepo).save(roadmap);
    }

    @Test
    @DisplayName("updateStepStatus : ACCEPTED compte aussi comme complété")
    void updateStepStatus_accepted_counts_as_completed() {
        RoadmapStep s1 = newStep(1, StepStatus.PENDING);
        RoadmapStep s2 = newStep(2, StepStatus.COMPLETED);
        Roadmap roadmap = newRoadmap(10L, 1L, 1001L, "Frontend", List.of(s1, s2));

        when(stepRepo.findByIdAndRoadmapUserId(1L, 1L)).thenReturn(Optional.of(s1));
        when(roadmapRepo.findByIdWithSteps(10L)).thenReturn(Optional.of(roadmap));
        when(roadmapMapper.toResponse(roadmap)).thenAnswer(inv ->
                new RoadmapResponse(10L, 1001L, "Frontend",
                        ((Roadmap) inv.getArgument(0)).getProgressPercentage(), List.of()));

        roadmapService.updateStepStatus(1L, 1L, new StepStatusUpdateDTO(StepStatus.ACCEPTED));

        assertThat(roadmap.getProgressPercentage()).isEqualTo(100);
    }

    @Test
    @DisplayName("updateStepStatus : roadmap sans steps → progress=0")
    void updateStepStatus_no_steps_progress_zero() {
        RoadmapStep s1 = newStep(1, StepStatus.PENDING);
        Roadmap roadmap = newRoadmap(10L, 1L, 1001L, "Frontend", List.of(s1));
        // Casser la liste exprès pour exercer le branch « steps.isEmpty() » :
        roadmap.getSteps().clear();
        s1.setRoadmap(roadmap);

        when(stepRepo.findByIdAndRoadmapUserId(1L, 1L)).thenReturn(Optional.of(s1));
        when(roadmapRepo.findByIdWithSteps(10L)).thenReturn(Optional.of(roadmap));
        when(roadmapMapper.toResponse(roadmap)).thenReturn(
                new RoadmapResponse(10L, 1001L, "Frontend", 0, List.of()));

        roadmapService.updateStepStatus(1L, 1L, new StepStatusUpdateDTO(StepStatus.COMPLETED));

        assertThat(roadmap.getProgressPercentage()).isZero();
    }

    @Test
    @DisplayName("updateStepStatus : step inconnu → StepNotFoundException")
    void updateStepStatus_throws_when_step_unknown() {
        when(stepRepo.findByIdAndRoadmapUserId(99L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roadmapService.updateStepStatus(1L, 99L,
                new StepStatusUpdateDTO(StepStatus.COMPLETED)))
                .isInstanceOf(StepNotFoundException.class);
    }

    // ─── getRoadmapById ────────────────────────────────────────────────

    @Test
    @DisplayName("getRoadmapById : succès")
    void getRoadmapById_success() {
        Roadmap roadmap = newRoadmap(10L, 1L, 1001L, "Frontend", List.of());
        when(roadmapRepo.findByIdWithSteps(10L)).thenReturn(Optional.of(roadmap));
        when(roadmapMapper.toResponse(roadmap)).thenReturn(responseFor(roadmap));

        RoadmapResponse result = roadmapService.getRoadmapById(1L, 10L);
        assertThat(result.id()).isEqualTo(10L);
    }

    @Test
    @DisplayName("getRoadmapById : refus si appartient à un autre user → RoadmapNotFoundException")
    void getRoadmapById_throws_when_user_mismatch() {
        Roadmap roadmap = newRoadmap(10L, 99L, 1001L, "Frontend", List.of());
        when(roadmapRepo.findByIdWithSteps(10L)).thenReturn(Optional.of(roadmap));

        assertThatThrownBy(() -> roadmapService.getRoadmapById(1L, 10L))
                .isInstanceOf(RoadmapNotFoundException.class);
    }

    @Test
    @DisplayName("getRoadmapById : id inconnu → RoadmapNotFoundException")
    void getRoadmapById_throws_when_id_unknown() {
        when(roadmapRepo.findByIdWithSteps(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roadmapService.getRoadmapById(1L, 99L))
                .isInstanceOf(RoadmapNotFoundException.class);
    }
}

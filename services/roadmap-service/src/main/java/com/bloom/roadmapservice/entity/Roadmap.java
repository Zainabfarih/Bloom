package com.bloom.roadmapservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "roadmap",
        uniqueConstraints = @UniqueConstraint(name = "uq_user_job", columnNames = {"user_id", "target_job_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Roadmap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "target_job_id", nullable = false)
    private Long targetJobId;

    @Column(name = "target_job_title", nullable = false)
    private String targetJobTitle;

    @Column(name = "progress_percentage")
    private Integer progressPercentage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "roadmap", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<RoadmapStep> steps = new ArrayList<>();

    @PrePersist
    private void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (progressPercentage == null) progressPercentage = 0;
    }
}
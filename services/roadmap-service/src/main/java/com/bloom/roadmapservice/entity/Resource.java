package com.bloom.roadmapservice.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "resource")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Resource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String url;

    private String type; // "video", "article", "course", "doc"

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "step_id", nullable = false)
    private RoadmapStep step;
}
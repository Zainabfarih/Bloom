package com.bloom.jobservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "saved_job",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_user_job",
                columnNames = {"user_id", "job_external_id"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavedJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "uuid", nullable = false, unique = true, updatable = false)
    private UUID uuid;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "cv_uuid", nullable = false, updatable = false)
    private UUID cvUuid;

    @Column(name = "job_external_id", nullable = false, length = 1024)
    private String jobExternalId;

    @Column(name = "job_title", nullable = false)
    private String jobTitle;

    @Column(name = "job_company")
    private String jobCompany;

    @Column(name = "job_location")
    private String jobLocation;

    @Column(name = "job_apply_url", columnDefinition = "TEXT")
    private String jobApplyUrl;

    @Column(name = "compatibility_score")
    private Integer compatibilityScore;

    @Column(name = "saved_at", nullable = false, updatable = false)
    private Instant savedAt;

    @OneToMany(
            mappedBy = "savedJob",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @Builder.Default
    private List<SavedJobSkill> skills = new ArrayList<>();

    @PrePersist
    private void prePersist() {
        if (uuid == null) uuid = UUID.randomUUID();
        if (savedAt == null) savedAt = Instant.now();
    }

    public void addSkills(List<String> names, SkillType type) {
        if (names == null) return;
        names.forEach(name -> skills.add(
                SavedJobSkill.builder()
                        .savedJob(this)
                        .skillName(name)
                        .skillType(type)
                        .build()
        ));
    }

    public List<String> getSkillsByType(SkillType type) {
        return skills.stream()
                .filter(s -> s.getSkillType() == type)
                .map(SavedJobSkill::getSkillName)
                .toList();
    }
}
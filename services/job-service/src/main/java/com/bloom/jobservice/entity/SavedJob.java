package com.bloom.jobservice.entity;

import com.bloom.jobservice.utils.StringArrayConverter;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "student_job",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_student_job",
                columnNames = {"student_id", "job_external_id"}
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

    @Column(name = "job_external_id", nullable = false)
    private String jobExternalId;

    @Column(name = "job_title", nullable = false)
    private String jobTitle;

    @Column(name = "job_company")
    private String jobCompany;

    @Column(name = "job_location")
    private String jobLocation;

    @Column(name = "job_apply_url", columnDefinition = "TEXT")
    private String jobApplyUrl;

    @Convert(converter = StringArrayConverter.class)
    @Column(name = "required_skills")
    private String[] requiredSkills;

    @Convert(converter = StringArrayConverter.class)
    @Column(name = "matched_skills")
    private String[] matchedSkills;

    @Convert(converter = StringArrayConverter.class)
    @Column(name = "missing_skills")
    private String[] missingSkills;

    @Column(name = "compatibility_score")
    private Integer compatibilityScore;

    @Column(name = "saved_at", nullable = false, updatable = false)
    private Instant savedAt;

    @PrePersist
    private void prePersist() {
        if (uuid == null) uuid = UUID.randomUUID();
        if (savedAt == null) savedAt = Instant.now();
    }
}

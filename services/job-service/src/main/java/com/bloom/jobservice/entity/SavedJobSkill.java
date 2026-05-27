package com.bloom.jobservice.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "saved_job_skill")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(SavedJobSkillId.class)
public class SavedJobSkill {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "saved_job_id", nullable = false)
    private SavedJob savedJob;

    @Id
    @Column(name = "skill_name", nullable = false, length = 100)
    private String skillName;

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "skill_type", nullable = false)
    private SkillType skillType;
}
package com.bloom.cvservice.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "cv_skill")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(CvSkillId.class)
public class CvSkill {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cv_id", nullable = false)
    private Cv cv;

    @Id
    @Column(name = "skill_name", nullable = false, length = 100)
    private String skillName;
}

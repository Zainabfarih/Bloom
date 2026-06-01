package com.bloom.jobservice.entity;

import java.io.Serializable;
import java.util.Objects;

public class SavedJobSkillId implements Serializable {

    private Long savedJob;
    private String skillName;
    private SkillType skillType;

    public SavedJobSkillId() {}

    public SavedJobSkillId(Long savedJob, String skillName, SkillType skillType) {
        this.savedJob = savedJob;
        this.skillName = skillName;
        this.skillType = skillType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SavedJobSkillId that)) return false;
        return Objects.equals(savedJob, that.savedJob)
                && Objects.equals(skillName, that.skillName)
                && skillType == that.skillType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(savedJob, skillName, skillType);
    }
}
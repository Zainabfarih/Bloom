package com.bloom.cvservice.entity;

import java.io.Serializable;
import java.util.Objects;

public class CvSkillId implements Serializable {

    private Long cv;
    private String skillName;

    public CvSkillId() {}

    public CvSkillId(Long cv, String skillName) {
        this.cv = cv;
        this.skillName = skillName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CvSkillId that)) return false;
        return Objects.equals(cv, that.cv)
                && Objects.equals(skillName, that.skillName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cv, skillName);
    }
}

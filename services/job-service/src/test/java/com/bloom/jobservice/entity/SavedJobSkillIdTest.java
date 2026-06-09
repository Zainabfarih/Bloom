package com.bloom.jobservice.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SavedJobSkillIdTest {

    @Test
    void equals_and_hashCode_follow_value_semantics() {
        SavedJobSkillId a = new SavedJobSkillId(1L, "Java", SkillType.REQUIRED);
        SavedJobSkillId b = new SavedJobSkillId(1L, "Java", SkillType.REQUIRED);
        SavedJobSkillId different = new SavedJobSkillId(1L, "Java", SkillType.MATCHED);

        assertThat(a).isEqualTo(b);
        assertThat(a).hasSameHashCodeAs(b);
        assertThat(a).isNotEqualTo(different);
        assertThat(a).isEqualTo(a);
        assertThat(a).isNotEqualTo(null);
        assertThat(a).isNotEqualTo("other-type");
    }
}

package com.bloom.cvservice.mapper;

import com.bloom.cvservice.dto.CvResponse;
import com.bloom.cvservice.dto.SkillsDTO;
import com.bloom.cvservice.entity.Cv;
import com.bloom.cvservice.entity.CvSource;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CvMapperTest {

    private final CvMapper mapper = new CvMapper();

    private Cv buildCv() {
        Cv cv = Cv.builder()
                .uuid(UUID.randomUUID())
                .userId(1L)
                .title("Mon CV")
                .source(CvSource.MANUAL)
                .originalFilename("cv.pdf")
                .active(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        cv.replaceSkills(List.of("Java", "Docker"));
        return cv;
    }

    @Test
    void toResponse_maps_all_fields() {
        Cv cv = buildCv();

        CvResponse response = mapper.toResponse(cv);

        assertThat(response.getUuid()).isEqualTo(cv.getUuid());
        assertThat(response.getTitle()).isEqualTo("Mon CV");
        assertThat(response.getSource()).isEqualTo(CvSource.MANUAL);
        assertThat(response.getOriginalFilename()).isEqualTo("cv.pdf");
        assertThat(response.getSkills()).containsExactly("Java", "Docker");
        assertThat(response.isActive()).isTrue();
    }

    @Test
    void toSkillsDTO_maps_user_uuid_and_skills() {
        Cv cv = buildCv();

        SkillsDTO dto = mapper.toSkillsDTO(cv);

        assertThat(dto.getUserId()).isEqualTo(1L);
        assertThat(dto.getCvUuid()).isEqualTo(cv.getUuid());
        assertThat(dto.getSkills()).containsExactly("Java", "Docker");
    }
}

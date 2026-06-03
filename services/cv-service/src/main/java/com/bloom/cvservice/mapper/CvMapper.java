package com.bloom.cvservice.mapper;

import com.bloom.cvservice.dto.CvResponse;
import com.bloom.cvservice.dto.SkillsDTO;
import com.bloom.cvservice.entity.Cv;
import org.springframework.stereotype.Component;

@Component
public class CvMapper {

    public CvResponse toResponse(Cv cv) {
        return CvResponse.builder()
                .uuid(cv.getUuid())
                .title(cv.getTitle())
                .source(cv.getSource())
                .originalFilename(cv.getOriginalFilename())
                .skills(cv.getSkillNames())
                .active(cv.isActive())
                .createdAt(cv.getCreatedAt())
                .updatedAt(cv.getUpdatedAt())
                .build();
    }

    public SkillsDTO toSkillsDTO(Cv cv) {
        return SkillsDTO.builder()
                .userId(cv.getUserId())
                .cvUuid(cv.getUuid())
                .skills(cv.getSkillNames())
                .build();
    }
}

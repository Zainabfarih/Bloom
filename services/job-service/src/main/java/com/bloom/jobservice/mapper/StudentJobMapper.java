package com.bloom.jobservice.mapper;

import com.bloom.jobservice.dto.SaveJobRequest;
import com.bloom.jobservice.dto.SavedJobResponse;
import com.bloom.jobservice.entity.StudentJob;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Arrays;
import java.util.List;

@Mapper(componentModel = "spring")
public interface StudentJobMapper {

    @Mapping(target = "id",      ignore = true)
    @Mapping(target = "uuid",    ignore = true)
    @Mapping(target = "savedAt", ignore = true)
    @Mapping(target = "studentId",      source = "studentId")
    @Mapping(target = "jobExternalId",  source = "req.jobExternalId")
    @Mapping(target = "jobTitle",       source = "req.jobTitle")
    @Mapping(target = "jobCompany",     source = "req.jobCompany")
    @Mapping(target = "jobLocation",    source = "req.jobLocation")
    @Mapping(target = "jobApplyUrl",    source = "req.jobApplyUrl")
    @Mapping(target = "requiredSkills",
            expression = "java(toArray(req.getRequiredSkills()))")
    @Mapping(target = "matchedSkills",
            expression = "java(toArray(req.getMatchedSkills()))")
    @Mapping(target = "missingSkills",
            expression = "java(toArray(req.getMissingSkills()))")
    @Mapping(target = "compatibilityScore", source = "req.compatibilityScore")
    StudentJob toEntity(SaveJobRequest req, Long studentId);

    @Mapping(target = "requiredSkills",
            expression = "java(toList(entity.getRequiredSkills()))")
    @Mapping(target = "matchedSkills",
            expression = "java(toList(entity.getMatchedSkills()))")
    @Mapping(target = "missingSkills",
            expression = "java(toList(entity.getMissingSkills()))")
    SavedJobResponse toResponse(StudentJob entity);

    default String[] toArray(List<String> list) {
        return list == null ? new String[0] : list.toArray(String[]::new);
    }

    default List<String> toList(String[] array) {
        return array == null ? List.of() : Arrays.asList(array);
    }
}
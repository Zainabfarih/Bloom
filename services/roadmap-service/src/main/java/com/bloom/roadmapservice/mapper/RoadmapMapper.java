package com.bloom.roadmapservice.mapper;

import com.bloom.roadmapservice.dto.ResourceDTO;
import com.bloom.roadmapservice.dto.RoadmapResponse;
import com.bloom.roadmapservice.dto.StepDTO;
import com.bloom.roadmapservice.entity.Resource;
import com.bloom.roadmapservice.entity.Roadmap;
import com.bloom.roadmapservice.entity.RoadmapStep;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface RoadmapMapper {

    // MapStruct mappe les records par constructeur canonique — les noms de champs doivent correspondre exactement
    @Mapping(source = "steps", target = "steps")
    RoadmapResponse toResponse(Roadmap roadmap);

    @Mapping(source = "resources", target = "resources")
    StepDTO toStepDto(RoadmapStep step);

    ResourceDTO toResourceDto(Resource resource);

    List<RoadmapResponse> toResponseList(List<Roadmap> roadmaps);
}
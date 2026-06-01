package com.bloom.jobservice.dto;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class SkillsDTO {

    private Long userId;

    private UUID cvUuid;

    private List<String> skills;
}
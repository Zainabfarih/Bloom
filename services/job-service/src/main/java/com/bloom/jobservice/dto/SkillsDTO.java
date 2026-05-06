package com.bloom.jobservice.dto;


import lombok.Data;

import java.util.List;

@Data
public class SkillsDTO {
    private Long studentId;
    private String jobTitle;
    private List<String> skills;
}
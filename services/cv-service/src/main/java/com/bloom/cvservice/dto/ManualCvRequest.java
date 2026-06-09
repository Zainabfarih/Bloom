package com.bloom.cvservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class ManualCvRequest {

    @Size(max = 255)
    private String title;

    @NotBlank(message = "summary is required")
    private String summary;

    @NotEmpty(message = "at least one experience is required")
    private List<@NotBlank(message = "experience entry cannot be blank") String> experiences;

    @NotEmpty(message = "at least one education entry is required")
    private List<@NotBlank(message = "education entry cannot be blank") String> educations;

    @NotEmpty(message = "at least one skill is required")
    private List<@NotBlank(message = "skill cannot be blank") String> skills;

    @NotBlank(message = "generated pdf is required")
    private String pdfBase64;
}

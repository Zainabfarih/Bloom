package com.bloom.cvservice.dto;

import com.bloom.cvservice.entity.CvSource;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class CvResponse {
    private UUID uuid;
    private String title;
    private CvSource source;
    private String originalFilename;
    private List<String> skills;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;
}

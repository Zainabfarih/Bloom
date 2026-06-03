package com.bloom.cvservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Contrat consommé par job-service via Feign
 * ({@code GET /api/cv/users/{userId}/skills} et {@code GET /api/cv/{cvUuid}/skills}).
 * La structure (userId, cvUuid, skills) doit rester strictement identique côté job-service.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillsDTO {

    private Long userId;

    private UUID cvUuid;

    private List<String> skills;
}

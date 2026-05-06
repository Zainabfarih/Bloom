package com.bloom.jobservice.repository;


import com.bloom.jobservice.entity.SavedJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SavedJobRepository extends JpaRepository<SavedJob, Long> {

    List<SavedJob> findByUserIdOrderByCompatibilityScoreDesc(Long userId);

    Optional<SavedJob> findByUserIdAndJobExternalId(Long userId, String jobExternalId);

    boolean existsByUserIdAndJobExternalId(Long userId, String jobExternalId);

    void deleteByUserIdAndJobExternalId(Long userId, String jobExternalId);

    Optional<SavedJob> findByUuid(UUID uuid);
}

package com.bloom.jobservice.repository;


import com.bloom.jobservice.entity.UserJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserJobRepository extends JpaRepository<UserJob, Long> {

    List<UserJob> findByUserIdOrderByCompatibilityScoreDesc(Long studentId);

    Optional<UserJob> findByUserIdAndJobExternalId(Long studentId, String jobExternalId);

    boolean existsByUserIdAndJobExternalId(Long studentId, String jobExternalId);

    void deleteByUserIdAndJobExternalId(Long studentId, String jobExternalId);

    Optional<UserJob> findByUuid(UUID uuid);
}

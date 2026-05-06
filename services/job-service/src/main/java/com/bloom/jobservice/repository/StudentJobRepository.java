package com.bloom.jobservice.repository;


import com.bloom.jobservice.entity.StudentJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StudentJobRepository extends JpaRepository<StudentJob, Long> {

    List<StudentJob> findByStudentIdOrderByCompatibilityScoreDesc(Long studentId);

    Optional<StudentJob> findByStudentIdAndJobExternalId(Long studentId, String jobExternalId);

    boolean existsByStudentIdAndJobExternalId(Long studentId, String jobExternalId);

    void deleteByStudentIdAndJobExternalId(Long studentId, String jobExternalId);

    Optional<StudentJob> findByUuid(UUID uuid);
}

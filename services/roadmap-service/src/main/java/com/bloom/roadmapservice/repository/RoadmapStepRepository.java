package com.bloom.roadmapservice.repository;

import com.bloom.roadmapservice.entity.RoadmapStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RoadmapStepRepository extends JpaRepository<RoadmapStep, Long> {

    // Charge step + son roadmap + tous les steps du roadmap
    // Resources (Set) chargées en batch secondaire par Hibernate → pas de MultipleBag
    @Query("""
        SELECT s FROM RoadmapStep s
        JOIN FETCH s.roadmap r
        LEFT JOIN FETCH r.steps
        WHERE s.id = :stepId AND r.userId = :userId
        """)
    Optional<RoadmapStep> findByIdAndRoadmapUserId(
            @Param("stepId") Long stepId,
            @Param("userId") Long userId
    );
}
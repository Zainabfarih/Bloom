package com.bloom.roadmapservice.repository;

import com.bloom.roadmapservice.entity.Roadmap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RoadmapRepository extends JpaRepository<Roadmap, Long> {

    // Requête 1 : charge Roadmap + steps (List<RoadmapStep>)
    // Hibernate charge ensuite resources (Set<Resource>) séparément via batch → pas de MultipleBag
    @Query("""
        SELECT DISTINCT r FROM Roadmap r
        LEFT JOIN FETCH r.steps
        WHERE r.userId = :userId
        ORDER BY r.createdAt DESC
        """)
    List<Roadmap> findByUserId(@Param("userId") Long userId);

    Optional<Roadmap> findByUserIdAndTargetJobId(Long userId, Long targetJobId);

    @Query("""
        SELECT DISTINCT r FROM Roadmap r
        LEFT JOIN FETCH r.steps
        WHERE r.id = :id
        """)
    Optional<Roadmap> findByIdWithSteps(@Param("id") Long id);
}
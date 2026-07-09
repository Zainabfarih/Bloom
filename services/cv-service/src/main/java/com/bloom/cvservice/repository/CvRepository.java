package com.bloom.cvservice.repository;

import com.bloom.cvservice.entity.Cv;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Accès aux CV en base (Spring Data JPA) avec chargement des skills. */
public interface CvRepository extends JpaRepository<Cv, Long> {

    @Query("SELECT DISTINCT c FROM Cv c LEFT JOIN FETCH c.skills " +
            "WHERE c.uuid = :uuid")
    Optional<Cv> findByUuidWithSkills(@Param("uuid") UUID uuid);

    @Query("SELECT DISTINCT c FROM Cv c LEFT JOIN FETCH c.skills " +
            "WHERE c.userId = :userId AND c.active = true")
    Optional<Cv> findActiveByUserIdWithSkills(@Param("userId") Long userId);

    @Query("SELECT c FROM Cv c WHERE c.userId = :userId ORDER BY c.updatedAt DESC")
    List<Cv> findByUserId(@Param("userId") Long userId);

    Optional<Cv> findByUserIdAndActiveTrue(Long userId);

    Optional<Cv> findByUuid(UUID uuid);
}

package com.bloom.jobservice.repository;

import com.bloom.jobservice.entity.SavedJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Accès aux offres sauvegardées (Spring Data JPA) avec chargement des skills. */
public interface SavedJobRepository extends JpaRepository<SavedJob, Long> {

    @Query("SELECT DISTINCT j FROM SavedJob j LEFT JOIN FETCH j.skills " +
            "WHERE j.userId = :userId " +
            "ORDER BY j.compatibilityScore DESC NULLS LAST")
    List<SavedJob> findByUserIdWithSkills(@Param("userId") Long userId);

    Optional<SavedJob> findByUserIdAndJobExternalId(Long userId, String jobExternalId);

    @Query("SELECT j FROM SavedJob j LEFT JOIN FETCH j.skills WHERE j.uuid = :uuid")
    Optional<SavedJob> findByUuidWithSkills(@Param("uuid") UUID uuid);


    @Modifying
    @Query("DELETE FROM SavedJob j WHERE j.userId = :userId AND j.jobExternalId = :jobExternalId")
    int deleteByUserIdAndJobExternalId(
            @Param("userId") Long userId,
            @Param("jobExternalId") String jobExternalId
    );
}
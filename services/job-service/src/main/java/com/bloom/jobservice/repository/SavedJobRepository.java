package com.bloom.jobservice.repository;

import com.bloom.jobservice.entity.SavedJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SavedJobRepository extends JpaRepository<SavedJob, Long> {

    /**
     * JOIN FETCH évite le problème N+1 : une seule requête SQL charge
     * l'entité + sa collection de skills. Sans ça, Hibernate ferait
     * 1 requête pour les SavedJob + N requêtes pour les skills de chaque.
     */
    @Query("SELECT DISTINCT j FROM SavedJob j LEFT JOIN FETCH j.skills " +
            "WHERE j.userId = :userId " +
            "ORDER BY j.compatibilityScore DESC NULLS LAST")
    List<SavedJob> findByUserIdWithSkills(@Param("userId") Long userId);

    Optional<SavedJob> findByUserIdAndJobExternalId(Long userId, String jobExternalId);

    @Query("SELECT j FROM SavedJob j LEFT JOIN FETCH j.skills WHERE j.uuid = :uuid")
    Optional<SavedJob> findByUuidWithSkills(@Param("uuid") UUID uuid);

    /**
     * CORRECTION : retourne int (nombre de lignes supprimées) au lieu de void.
     * Permet de détecter "job non trouvé" sans double requête existsBy + deleteBy.
     * @Modifying est obligatoire pour les DELETE/UPDATE JPQL.
     */
    @Modifying
    @Query("DELETE FROM SavedJob j WHERE j.userId = :userId AND j.jobExternalId = :jobExternalId")
    int deleteByUserIdAndJobExternalId(
            @Param("userId") Long userId,
            @Param("jobExternalId") String jobExternalId
    );
}
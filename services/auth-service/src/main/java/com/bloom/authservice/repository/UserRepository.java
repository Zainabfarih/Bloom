package com.bloom.authservice.repository;

import com.bloom.authservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    // ─── Admin views — bypass the @SQLRestriction("deleted = false") so soft-deleted
    //     users remain visible/restorable. Native queries are not affected by the filter. ───

    @Query(value = "SELECT * FROM users ORDER BY created_at DESC", nativeQuery = true)
    List<User> findAllIncludingDeleted();

    @Query(value = "SELECT * FROM users WHERE id = :id", nativeQuery = true)
    Optional<User> findByIdIncludingDeleted(@Param("id") Long id);

    @Query(value = "SELECT COUNT(*) FROM users", nativeQuery = true)
    long countAllIncludingDeleted();

    @Query(value = "SELECT COUNT(*) FROM users WHERE deleted = true", nativeQuery = true)
    long countDeleted();

    @Query(value = "SELECT COUNT(*) FROM users WHERE deleted = false AND enabled = true", nativeQuery = true)
    long countActive();

    @Query(value = "SELECT COUNT(*) FROM users WHERE role = :role AND deleted = false", nativeQuery = true)
    long countByRoleActive(@Param("role") String role);

    @Query(value = "SELECT COUNT(*) FROM users WHERE created_at >= :since", nativeQuery = true)
    long countCreatedSince(@Param("since") java.time.LocalDateTime since);
}

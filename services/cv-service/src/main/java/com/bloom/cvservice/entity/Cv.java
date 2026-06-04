package com.bloom.cvservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "cv",
        indexes = {
                @Index(name = "idx_cv_user_id", columnList = "user_id"),
                @Index(name = "idx_cv_user_active", columnList = "user_id, active")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cv {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "uuid", nullable = false, unique = true, updatable = false)
    private UUID uuid;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** Titre / intitulé donné au CV par l'étudiant. */
    @Column(name = "title", length = 255)
    private String title;

    /** Origine : UPLOAD (fichier PDF) ou MANUAL (saisie section par section). */
    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 20)
    private CvSource source;

    /** Nom du fichier PDF importé (null si saisie manuelle). */
    @Column(name = "original_filename", length = 255)
    private String originalFilename;

    /**
     * Texte brut du CV : extrait du PDF à l'upload,
     * ou assemblé depuis les sections saisies manuellement.
     * Sert de base à l'extraction de skills et à l'analyse ATS (à la volée).
     */
    @Column(name = "raw_text", columnDefinition = "TEXT")
    private String rawText;

    /** Un seul CV actif par étudiant — celui utilisé pour le matching emploi. */
    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "cv", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<CvSkill> skills = new ArrayList<>();

    @PrePersist
    private void prePersist() {
        if (uuid == null) uuid = UUID.randomUUID();
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    private void preUpdate() {
        updatedAt = Instant.now();
    }

    // ─── Helpers de gestion des skills ───────────────────────────────────────

    public void replaceSkills(List<String> names) {
        skills.clear();
        if (names == null) return;
        names.stream()
                .filter(n -> n != null && !n.isBlank())
                .map(String::trim)
                .distinct()
                .forEach(name -> skills.add(
                        CvSkill.builder().cv(this).skillName(name).build()));
    }

    public List<String> getSkillNames() {
        return skills.stream().map(CvSkill::getSkillName).toList();
    }
}

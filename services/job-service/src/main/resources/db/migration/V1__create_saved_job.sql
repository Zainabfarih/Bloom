-- ══════════════════════════════════════════════════════════════════════
-- V1 : Création des tables saved_job et saved_job_skill
-- ══════════════════════════════════════════════════════════════════════

-- ── Table principale ───────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS saved_job (
                                         id                  BIGSERIAL PRIMARY KEY,

    -- UUID métier généré côté Java
                                         uuid                UUID NOT NULL UNIQUE,

    -- ID de l'étudiant
                                         user_id             BIGINT NOT NULL,

    -- UUID du CV utilisé pour le matching
                                         cv_uuid             UUID NOT NULL,

                                         job_external_id     VARCHAR(255) NOT NULL,
                                         job_title           VARCHAR(500) NOT NULL,
                                         job_company         VARCHAR(255),
                                         job_location        VARCHAR(255),
                                         job_apply_url       TEXT,

                                         compatibility_score SMALLINT
                                             CHECK (compatibility_score BETWEEN 0 AND 100),

                                         saved_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),

                                         CONSTRAINT uq_user_job
                                             UNIQUE (user_id, job_external_id)
);

-- Index principal
CREATE INDEX IF NOT EXISTS idx_saved_job_user_id
    ON saved_job(user_id);

-- Tri par score
CREATE INDEX IF NOT EXISTS idx_saved_job_user_score
    ON saved_job(user_id, compatibility_score DESC NULLS LAST)
    WHERE compatibility_score IS NOT NULL;

-- Recherche par CV
CREATE INDEX IF NOT EXISTS idx_saved_job_cv_uuid
    ON saved_job(cv_uuid);

-- ── Skills ────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS saved_job_skill (
                                               saved_job_id BIGINT NOT NULL
                                                   REFERENCES saved_job(id) ON DELETE CASCADE,

                                               skill_name   VARCHAR(100) NOT NULL,

                                               skill_type   VARCHAR(20) NOT NULL
                                                   CHECK (skill_type IN ('REQUIRED', 'MATCHED', 'MISSING')),

                                               PRIMARY KEY (saved_job_id, skill_name, skill_type)
);

CREATE INDEX IF NOT EXISTS idx_skill_job_id
    ON saved_job_skill(saved_job_id);

CREATE INDEX IF NOT EXISTS idx_skill_name_type
    ON saved_job_skill(skill_name, skill_type);
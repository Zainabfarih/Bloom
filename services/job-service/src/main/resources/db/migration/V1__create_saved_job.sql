-- ══════════════════════════════════════════════════════════════════════
-- V1 : Création des tables saved_job et saved_job_skill
-- cv_uuid NOT NULL : chaque saved_job est lié à un CV spécifique
-- uuid, user_id, cv_uuid : tous NOT NULL
-- ══════════════════════════════════════════════════════════════════════

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ── Table principale ───────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS saved_job (
                                         id                  BIGSERIAL       PRIMARY KEY,

    -- uuid : identifiant métier exposé dans les APIs (jamais l'id interne)
                                         uuid                UUID            NOT NULL DEFAULT uuid_generate_v4() UNIQUE,

    -- user_id : ID de l'étudiant (vient du header X-User-Id injecté par gateway)
                                         user_id             BIGINT          NOT NULL,

    -- cv_uuid : UUID du CV (cv-service) utilisé pour le skill matching
    --           NOT NULL : on ne sauvegarde un job que si le matching a pu être calculé
                                         cv_uuid             UUID            NOT NULL,

                                         job_external_id     VARCHAR(255)    NOT NULL,
                                         job_title           VARCHAR(500)    NOT NULL,
                                         job_company         VARCHAR(255),
                                         job_location        VARCHAR(255),
                                         job_apply_url       TEXT,
                                         compatibility_score SMALLINT        CHECK (compatibility_score BETWEEN 0 AND 100),
                                         saved_at            TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    -- Un user ne peut sauvegarder le même job qu'une seule fois
                                         CONSTRAINT uq_user_job UNIQUE (user_id, job_external_id)
);

-- Index : findByUserIdWithSkills (requête principale)
CREATE INDEX IF NOT EXISTS idx_saved_job_user_id
    ON saved_job (user_id);

-- Index : tri par score de compatibilité (ORDER BY score DESC)
CREATE INDEX IF NOT EXISTS idx_saved_job_user_score
    ON saved_job (user_id, compatibility_score DESC NULLS LAST)
    WHERE compatibility_score IS NOT NULL;

-- Index : retrouver tous les jobs liés à un CV donné (roadmap-service)
CREATE INDEX IF NOT EXISTS idx_saved_job_cv_uuid
    ON saved_job (cv_uuid);

-- ── Table des skills extraits et matchés ──────────────────────────────
CREATE TABLE IF NOT EXISTS saved_job_skill (
                                               saved_job_id    BIGINT       NOT NULL
                                                   REFERENCES saved_job(id) ON DELETE CASCADE,
                                               skill_name      VARCHAR(100) NOT NULL,
                                               skill_type      VARCHAR(20)  NOT NULL
                                                   CHECK (skill_type IN ('REQUIRED', 'MATCHED', 'MISSING')),

                                               PRIMARY KEY (saved_job_id, skill_name, skill_type)
);

CREATE INDEX IF NOT EXISTS idx_skill_job_id
    ON saved_job_skill (saved_job_id);

CREATE INDEX IF NOT EXISTS idx_skill_name_type
    ON saved_job_skill (skill_name, skill_type);
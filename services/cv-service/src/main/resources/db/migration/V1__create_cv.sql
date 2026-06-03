-- ══════════════════════════════════════════════════════════════════════
-- V1 : Création des tables cv et cv_skill
-- On stocke uniquement : le CV (PDF ou saisie manuelle), ses skills,
-- et l'utilisateur associé. Aucune analyse ATS n'est persistée.
-- ══════════════════════════════════════════════════════════════════════

-- ── CV ─────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS cv (
    id                BIGSERIAL PRIMARY KEY,

    -- UUID métier généré côté Java (exposé aux autres services)
    uuid              UUID NOT NULL UNIQUE,

    -- ID de l'étudiant propriétaire
    user_id           BIGINT NOT NULL,

    title             VARCHAR(255),

    -- UPLOAD (fichier PDF) ou MANUAL (saisie section par section)
    source            VARCHAR(20) NOT NULL
                          CHECK (source IN ('UPLOAD', 'MANUAL')),

    original_filename VARCHAR(255),

    -- Texte brut : extrait du PDF ou assemblé depuis la saisie manuelle
    raw_text          TEXT,

    -- Un seul CV actif par étudiant (celui utilisé pour le matching emploi)
    active            BOOLEAN NOT NULL DEFAULT TRUE,

    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_cv_user_id
    ON cv(user_id);

-- Un seul CV actif par étudiant
CREATE UNIQUE INDEX IF NOT EXISTS uq_cv_user_active
    ON cv(user_id)
    WHERE active = TRUE;

-- ── Skills extraits du CV ────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS cv_skill (
    cv_id      BIGINT NOT NULL
                   REFERENCES cv(id) ON DELETE CASCADE,

    skill_name VARCHAR(100) NOT NULL,

    PRIMARY KEY (cv_id, skill_name)
);

CREATE INDEX IF NOT EXISTS idx_cv_skill_cv_id
    ON cv_skill(cv_id);

CREATE INDEX IF NOT EXISTS idx_cv_skill_name
    ON cv_skill(skill_name);

-- V1 : tables cv et cv_skill.
-- On stocke le fichier PDF (uploadé ou généré depuis la saisie manuelle),
-- pas le texte brut. L'analyse ATS est calculée à la volée, non persistée.

DROP TABLE IF EXISTS cv_skill CASCADE;
DROP TABLE IF EXISTS cv CASCADE;

CREATE TABLE cv (
    id                BIGSERIAL PRIMARY KEY,
    uuid              UUID NOT NULL UNIQUE,
    user_id           BIGINT NOT NULL,
    title             VARCHAR(255),

    source            VARCHAR(20) NOT NULL
                          CHECK (source IN ('UPLOAD', 'MANUAL')),

    -- Fichier PDF stocké
    original_filename VARCHAR(255),
    content_type      VARCHAR(100) NOT NULL DEFAULT 'application/pdf',
    file_data         BYTEA,
    file_size         BIGINT,

    -- Texte (sections assemblées) utilisé avec les skills pour l'analyse ATS
    description       TEXT,

    active            BOOLEAN NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_cv_user_id ON cv(user_id);

-- Un seul CV actif par étudiant
CREATE UNIQUE INDEX uq_cv_user_active ON cv(user_id) WHERE active = TRUE;

-- Skills extraits : une ligne par skill
CREATE TABLE cv_skill (
    cv_id      BIGINT NOT NULL REFERENCES cv(id) ON DELETE CASCADE,
    skill_name VARCHAR(100) NOT NULL,
    PRIMARY KEY (cv_id, skill_name)
);

CREATE INDEX idx_cv_skill_cv_id ON cv_skill(cv_id);
CREATE INDEX idx_cv_skill_name ON cv_skill(skill_name);

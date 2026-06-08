-- ══════════════════════════════════════════════════════════════════════
-- V1 : Schéma initial du roadmap-service
--   • roadmap        : un parcours d'apprentissage par étudiant + job ciblé
--   • roadmap_step   : étapes ordonnées d'un roadmap, avec statut
--   • resource       : ressources pédagogiques attachées à une étape
-- ══════════════════════════════════════════════════════════════════════

-- ── Table roadmap ─────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS roadmap (
    id                   BIGSERIAL PRIMARY KEY,
    user_id              BIGINT       NOT NULL,
    target_job_id        BIGINT       NOT NULL,
    target_job_title     VARCHAR(255) NOT NULL,
    progress_percentage  INTEGER      NOT NULL DEFAULT 0
                            CHECK (progress_percentage BETWEEN 0 AND 100),
    created_at           TIMESTAMP    NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_user_job UNIQUE (user_id, target_job_id)
);

CREATE INDEX IF NOT EXISTS idx_roadmap_user_id ON roadmap(user_id);

-- ── Table roadmap_step ────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS roadmap_step (
    id                  BIGSERIAL PRIMARY KEY,
    roadmap_id          BIGINT       NOT NULL,
    title               VARCHAR(255) NOT NULL,
    description         TEXT,
    order_index         INTEGER      NOT NULL,
    status              VARCHAR(20)  NOT NULL DEFAULT 'PENDING'
                            CHECK (status IN ('PENDING', 'IN_PROGRESS', 'COMPLETED', 'ACCEPTED', 'REJECTED')),
    estimated_duration  VARCHAR(50),

    CONSTRAINT fk_step_roadmap
        FOREIGN KEY (roadmap_id) REFERENCES roadmap(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_step_roadmap_id  ON roadmap_step(roadmap_id);
CREATE INDEX IF NOT EXISTS idx_step_roadmap_order
    ON roadmap_step(roadmap_id, order_index);

-- ── Table resource ────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS resource (
    id        BIGSERIAL PRIMARY KEY,
    step_id   BIGINT       NOT NULL,
    title     VARCHAR(255) NOT NULL,
    url       TEXT,
    type      VARCHAR(50),    -- "video", "article", "course", "doc"

    CONSTRAINT fk_resource_step
        FOREIGN KEY (step_id) REFERENCES roadmap_step(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_resource_step_id ON resource(step_id);

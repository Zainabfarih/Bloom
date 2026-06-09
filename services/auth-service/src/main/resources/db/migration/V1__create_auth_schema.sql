-- ══════════════════════════════════════════════════════════════════════
-- V1 : Schéma initial du auth-service
--   • users                   : étudiants + admins (soft-delete)
--   • refresh_tokens          : JWT refresh tokens (rotation)
--   • password_reset_tokens   : tokens de réinitialisation par email
-- ══════════════════════════════════════════════════════════════════════

-- ── Table users ───────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS users (
    id                      BIGSERIAL PRIMARY KEY,

    first_name              VARCHAR(255) NOT NULL,
    last_name               VARCHAR(255) NOT NULL,

    email                   VARCHAR(255) NOT NULL,
    password                VARCHAR(255) NOT NULL,           -- BCrypt hash (60 chars)

    enabled                 BOOLEAN      NOT NULL DEFAULT TRUE,
    locked                  BOOLEAN      NOT NULL DEFAULT FALSE,
    failed_login_attempts   INTEGER      NOT NULL DEFAULT 0,
    deleted                 BOOLEAN      NOT NULL DEFAULT FALSE,

    role                    VARCHAR(20)  NOT NULL DEFAULT 'STUDENT'
                                CHECK (role IN ('STUDENT', 'ADMIN')),

    created_at              TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- Email unique seulement parmi les utilisateurs non supprimés
-- (permet de réinscrire un email après soft-delete).
CREATE UNIQUE INDEX IF NOT EXISTS uq_users_email_active
    ON users(email)
    WHERE deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_users_role
    ON users(role)
    WHERE deleted = FALSE;

-- ── Table refresh_tokens ──────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id           BIGSERIAL PRIMARY KEY,
    token        VARCHAR(512) NOT NULL UNIQUE,
    expiry_date  TIMESTAMPTZ  NOT NULL,
    revoked      BOOLEAN      NOT NULL DEFAULT FALSE,
    user_id      BIGINT       NOT NULL,

    CONSTRAINT fk_refresh_token_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_id
    ON refresh_tokens(user_id);

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_active
    ON refresh_tokens(user_id, expiry_date)
    WHERE revoked = FALSE;

-- ── Table password_reset_tokens ───────────────────────────────────────
CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id           BIGSERIAL PRIMARY KEY,
    token        VARCHAR(512) NOT NULL UNIQUE,
    expiry_date  TIMESTAMPTZ  NOT NULL,
    used         BOOLEAN      NOT NULL DEFAULT FALSE,
    user_id      BIGINT       NOT NULL,

    CONSTRAINT fk_pwd_reset_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_pwd_reset_user_id
    ON password_reset_tokens(user_id);

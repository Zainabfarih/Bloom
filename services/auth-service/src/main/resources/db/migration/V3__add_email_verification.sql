-- ══════════════════════════════════════════════════════════════════════
-- V3 : Vérification d'email à l'inscription
--   • Ajoute users.email_verified
--   • Crée la table email_verification_tokens (mêmes propriétés que
--     password_reset_tokens : token unique, expiration, used flag, FK user)
-- ══════════════════════════════════════════════════════════════════════

-- ── Colonne email_verified ────────────────────────────────────────────
-- Ajout en deux temps pour ne pas casser les lignes existantes :
--   1) ajout nullable, 2) backfill TRUE sur les comptes existants
--      (les users seedés en V2 sont considérés vérifiés),
--   3) NOT NULL + DEFAULT FALSE pour les nouvelles inscriptions.
ALTER TABLE users ADD COLUMN IF NOT EXISTS email_verified BOOLEAN;

UPDATE users SET email_verified = TRUE WHERE email_verified IS NULL;

ALTER TABLE users ALTER COLUMN email_verified SET NOT NULL;
ALTER TABLE users ALTER COLUMN email_verified SET DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_users_email_verified
    ON users(email_verified)
    WHERE deleted = FALSE;

-- ── Table email_verification_tokens ───────────────────────────────────
CREATE TABLE IF NOT EXISTS email_verification_tokens (
    id           BIGSERIAL PRIMARY KEY,
    token        VARCHAR(512) NOT NULL UNIQUE,
    expiry_date  TIMESTAMPTZ  NOT NULL,
    used         BOOLEAN      NOT NULL DEFAULT FALSE,
    user_id      BIGINT       NOT NULL,

    CONSTRAINT fk_email_verification_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_email_verification_user_id
    ON email_verification_tokens(user_id);

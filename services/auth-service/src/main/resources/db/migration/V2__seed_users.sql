-- ══════════════════════════════════════════════════════════════════════
-- V2 : Seed data — comptes de démonstration
--
-- Les mots de passe sont hashés avec BCrypt strength=12, comme configuré
-- dans com.bloom.authservice.config.SecurityConfig#passwordEncoder().
--
-- Comptes créés :
--   admin@bloom.dev    / admin123     (ADMIN)
--   alice@bloom.dev    / student123   (STUDENT)
--   bob@bloom.dev      / student123   (STUDENT)
--   carol@bloom.dev    / Password1!   (STUDENT — compte locked pour tests)
--
-- ⚠  À utiliser uniquement en dev / staging — JAMAIS en production.
-- ══════════════════════════════════════════════════════════════════════

INSERT INTO users (first_name, last_name, email, password, role, enabled, locked, failed_login_attempts, deleted)
VALUES
    ('Admin',  'Bloom',    'admin@bloom.dev',
     '$2a$12$2unHmf7pzTHfSWlkWh.oieMFQGxfCOwnhgtN30.w7l/dwuPMI.ep.',
     'ADMIN',   TRUE,  FALSE, 0, FALSE),

    ('Alice',  'Martin',   'alice@bloom.dev',
     '$2a$12$qFSEz3YjoUhzCgdeHem2NO1FGEIR0DzZWIQvNfg31tKYCnM4VvZwC',
     'STUDENT', TRUE,  FALSE, 0, FALSE),

    ('Bob',    'Durand',   'bob@bloom.dev',
     '$2a$12$qFSEz3YjoUhzCgdeHem2NO1FGEIR0DzZWIQvNfg31tKYCnM4VvZwC',
     'STUDENT', TRUE,  FALSE, 0, FALSE),

    ('Carol',  'Bensaid',  'carol@bloom.dev',
     '$2a$12$coftRsVPzAUL6i1yCl/.OOuSM59/aB6x24ebqyO184h.kA0PwBfRe',
     'STUDENT', TRUE,  TRUE,  5, FALSE)
ON CONFLICT DO NOTHING;

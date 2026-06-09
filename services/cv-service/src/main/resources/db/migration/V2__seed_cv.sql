-- V2 : donnees de demonstration (un CV manuel actif pour l'utilisateur 1)
-- Idempotent via ON CONFLICT pour ne pas echouer si rejouee.

INSERT INTO cv (uuid, user_id, title, source, original_filename, content_type,
                file_data, file_size, description, active)
VALUES (
    '11111111-1111-1111-1111-111111111111',
    1,
    'CV Demo — Software Engineer',
    'MANUAL',
    'cv-demo-software-engineer.pdf',
    'application/pdf',
    NULL,
    NULL,
    E'SUMMARY\nEtudiant en genie logiciel, oriente backend et cloud.\n\n'
    || E'EXPERIENCE\nStage backend (6 mois) — API REST Spring Boot, PostgreSQL.\n\n'
    || E'EDUCATION\nMaster Genie Logiciel — 2026\n\n'
    || E'SKILLS\nJava, Spring Boot, PostgreSQL, Docker, React',
    TRUE
)
ON CONFLICT (uuid) DO NOTHING;

INSERT INTO cv_skill (cv_id, skill_name)
SELECT c.id, s.skill_name
FROM cv c
CROSS JOIN (VALUES
    ('Java'),
    ('Spring Boot'),
    ('PostgreSQL'),
    ('Docker'),
    ('React')
) AS s(skill_name)
WHERE c.uuid = '11111111-1111-1111-1111-111111111111'
ON CONFLICT (cv_id, skill_name) DO NOTHING;

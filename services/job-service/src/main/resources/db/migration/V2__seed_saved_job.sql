-- V2 : donnees de demonstration (une offre sauvegardee pour l'utilisateur 1)
-- Idempotent via ON CONFLICT pour ne pas echouer si rejouee.

INSERT INTO saved_job (uuid, user_id, cv_uuid, job_external_id, job_title,
                       job_company, job_location, job_apply_url, compatibility_score)
VALUES (
    '22222222-2222-2222-2222-222222222222',
    1,
    '11111111-1111-1111-1111-111111111111',
    'demo-job-001',
    'Backend Java Developer',
    'Bloom Demo Corp',
    'Rabat, Morocco',
    'https://example.com/apply/demo-job-001',
    60
)
ON CONFLICT (uuid) DO NOTHING;

INSERT INTO saved_job_skill (saved_job_id, skill_name, skill_type)
SELECT j.id, v.skill_name, v.skill_type
FROM saved_job j
CROSS JOIN (VALUES
    ('Java', 'REQUIRED'),
    ('Spring Boot', 'REQUIRED'),
    ('Docker', 'REQUIRED'),
    ('Java', 'MATCHED'),
    ('Spring Boot', 'MATCHED'),
    ('Docker', 'MISSING')
) AS v(skill_name, skill_type)
WHERE j.uuid = '22222222-2222-2222-2222-222222222222'
ON CONFLICT (saved_job_id, skill_name, skill_type) DO NOTHING;

-- ══════════════════════════════════════════════════════════════════════
-- V2 : Seed data — jobs sauvegardés de démonstration
--
-- Hypothèses :
--   • user_id 2 = Alice — CV uuid 11111111-1111-1111-1111-111111111111
--   • user_id 3 = Bob   — CV uuid 22222222-2222-2222-2222-222222222222
--     (cf. cv-service.V2__seed_cv.sql)
--
-- Les job_external_id correspondent à des IDs SerpAPI fictifs ; en démo,
-- ils permettent de tester la liste des favoris sans appeler l'API externe.
--
-- ⚠  Dev / staging uniquement.
-- ══════════════════════════════════════════════════════════════════════

-- ── Job sauvegardé 1 : Alice → Frontend Developer @ TechMinds ─────────
INSERT INTO saved_job (uuid, user_id, cv_uuid, job_external_id, job_title,
                       job_company, job_location, job_apply_url, compatibility_score)
VALUES (
    'aaaaaaaa-1111-1111-1111-aaaaaaaaaaaa',
    2,
    '11111111-1111-1111-1111-111111111111',
    'serp-demo-frontend-001',
    'Frontend Developer (React)',
    'TechMinds',
    'Casablanca, Morocco',
    'https://example.com/jobs/serp-demo-frontend-001',
    78
)
ON CONFLICT (user_id, job_external_id) DO NOTHING;

INSERT INTO saved_job_skill (saved_job_id, skill_name, skill_type)
SELECT id, s.skill, s.kind FROM saved_job,
    (VALUES
        ('html',        'REQUIRED'),
        ('css',         'REQUIRED'),
        ('javascript',  'REQUIRED'),
        ('typescript',  'REQUIRED'),
        ('react',       'REQUIRED'),
        ('next.js',     'REQUIRED'),
        ('redux',       'REQUIRED'),
        -- skills présents dans le CV d'Alice
        ('html',        'MATCHED'),
        ('css',         'MATCHED'),
        ('javascript',  'MATCHED'),
        ('typescript',  'MATCHED'),
        ('react',       'MATCHED'),
        -- skills manquants (gap analysis)
        ('next.js',     'MISSING'),
        ('redux',       'MISSING')
    ) AS s(skill, kind)
WHERE job_external_id = 'serp-demo-frontend-001'
ON CONFLICT DO NOTHING;

-- ── Job sauvegardé 2 : Alice → UI Engineer @ Pixel Labs ──────────────
INSERT INTO saved_job (uuid, user_id, cv_uuid, job_external_id, job_title,
                       job_company, job_location, job_apply_url, compatibility_score)
VALUES (
    'aaaaaaaa-2222-2222-2222-aaaaaaaaaaaa',
    2,
    '11111111-1111-1111-1111-111111111111',
    'serp-demo-ui-002',
    'Junior UI Engineer',
    'Pixel Labs',
    'Remote — EU',
    'https://example.com/jobs/serp-demo-ui-002',
    65
)
ON CONFLICT (user_id, job_external_id) DO NOTHING;

INSERT INTO saved_job_skill (saved_job_id, skill_name, skill_type)
SELECT id, s.skill, s.kind FROM saved_job,
    (VALUES
        ('html',        'REQUIRED'),
        ('css',         'REQUIRED'),
        ('javascript',  'REQUIRED'),
        ('figma',       'REQUIRED'),
        ('accessibility','REQUIRED'),
        ('storybook',   'REQUIRED'),
        ('html',        'MATCHED'),
        ('css',         'MATCHED'),
        ('javascript',  'MATCHED'),
        ('figma',       'MATCHED'),
        ('accessibility','MISSING'),
        ('storybook',   'MISSING')
    ) AS s(skill, kind)
WHERE job_external_id = 'serp-demo-ui-002'
ON CONFLICT DO NOTHING;

-- ── Job sauvegardé 3 : Bob → Data Analyst @ MarketScope ──────────────
INSERT INTO saved_job (uuid, user_id, cv_uuid, job_external_id, job_title,
                       job_company, job_location, job_apply_url, compatibility_score)
VALUES (
    'bbbbbbbb-1111-1111-1111-bbbbbbbbbbbb',
    3,
    '22222222-2222-2222-2222-222222222222',
    'serp-demo-data-101',
    'Data Analyst Junior',
    'MarketScope',
    'Rabat, Morocco',
    'https://example.com/jobs/serp-demo-data-101',
    71
)
ON CONFLICT (user_id, job_external_id) DO NOTHING;

INSERT INTO saved_job_skill (saved_job_id, skill_name, skill_type)
SELECT id, s.skill, s.kind FROM saved_job,
    (VALUES
        ('sql',         'REQUIRED'),
        ('python',      'REQUIRED'),
        ('pandas',      'REQUIRED'),
        ('excel',       'REQUIRED'),
        ('power bi',    'REQUIRED'),
        ('statistics',  'REQUIRED'),
        ('sql',         'MATCHED'),
        ('python',      'MATCHED'),
        ('pandas',      'MATCHED'),
        ('excel',       'MATCHED'),
        ('power bi',    'MISSING'),
        ('statistics',  'MISSING')
    ) AS s(skill, kind)
WHERE job_external_id = 'serp-demo-data-101'
ON CONFLICT DO NOTHING;

-- ── Job sauvegardé 4 : Bob → Business Intelligence Intern @ DataNova ─
INSERT INTO saved_job (uuid, user_id, cv_uuid, job_external_id, job_title,
                       job_company, job_location, job_apply_url, compatibility_score)
VALUES (
    'bbbbbbbb-2222-2222-2222-bbbbbbbbbbbb',
    3,
    '22222222-2222-2222-2222-222222222222',
    'serp-demo-bi-102',
    'Business Intelligence Intern',
    'DataNova',
    'Casablanca, Morocco',
    'https://example.com/jobs/serp-demo-bi-102',
    58
)
ON CONFLICT (user_id, job_external_id) DO NOTHING;

INSERT INTO saved_job_skill (saved_job_id, skill_name, skill_type)
SELECT id, s.skill, s.kind FROM saved_job,
    (VALUES
        ('sql',         'REQUIRED'),
        ('tableau',     'REQUIRED'),
        ('excel',       'REQUIRED'),
        ('etl',         'REQUIRED'),
        ('dax',         'REQUIRED'),
        ('sql',         'MATCHED'),
        ('excel',       'MATCHED'),
        ('tableau',     'MISSING'),
        ('etl',         'MISSING'),
        ('dax',         'MISSING')
    ) AS s(skill, kind)
WHERE job_external_id = 'serp-demo-bi-102'
ON CONFLICT DO NOTHING;

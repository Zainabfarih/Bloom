-- ══════════════════════════════════════════════════════════════════════
-- V2 : Seed data — roadmaps de démonstration
--
-- Hypothèses (data isolation : chaque service a son propre schéma) :
--   • user_id 2 = Alice (seed du auth-service, V2__seed_users.sql)
--   • user_id 3 = Bob   (seed du auth-service, V2__seed_users.sql)
--   • target_job_id : ID logique côté job-service (libre — démo only)
--
-- Démonstration :
--   • Alice prépare un poste "Frontend Developer" — déjà en cours
--   • Bob   prépare un poste "Data Analyst"      — fraîchement créé
--
-- ⚠  Dev / staging uniquement.
-- ══════════════════════════════════════════════════════════════════════

-- ── Roadmap 1 : Alice → Frontend Developer ────────────────────────────
INSERT INTO roadmap (id, user_id, target_job_id, target_job_title, progress_percentage)
VALUES (1, 2, 1001, 'Frontend Developer', 33)
ON CONFLICT (user_id, target_job_id) DO NOTHING;

INSERT INTO roadmap_step (id, roadmap_id, title, description, order_index, status, estimated_duration)
VALUES
    (1, 1, 'Maîtriser HTML & CSS',
        'Sémantique HTML5, Flexbox, Grid, responsive design.',
        1, 'COMPLETED',  '2 semaines'),
    (2, 1, 'JavaScript moderne',
        'ES6+, async/await, modules, DOM API.',
        2, 'IN_PROGRESS', '3 semaines'),
    (3, 1, 'React 18',
        'Hooks, Context, React Router, gestion d''état.',
        3, 'PENDING',    '4 semaines'),
    (4, 1, 'Tests & outillage',
        'Vitest, Testing Library, ESLint, Vite.',
        4, 'PENDING',    '2 semaines')
ON CONFLICT (id) DO NOTHING;

INSERT INTO resource (step_id, title, url, type) VALUES
    (1, 'MDN — Apprendre HTML',          'https://developer.mozilla.org/fr/docs/Learn/HTML',    'doc'),
    (1, 'CSS Tricks — A Complete Guide to Flexbox',
        'https://css-tricks.com/snippets/css/a-guide-to-flexbox/',                                'article'),
    (2, 'JavaScript.info',               'https://javascript.info/',                            'course'),
    (2, 'Eloquent JavaScript',           'https://eloquentjavascript.net/',                     'doc'),
    (3, 'React Docs (Beta)',             'https://react.dev/learn',                             'doc'),
    (3, 'Epic React (Kent C. Dodds)',    'https://epicreact.dev/',                              'course'),
    (4, 'Testing Library Docs',          'https://testing-library.com/docs/',                   'doc')
;

-- ── Roadmap 2 : Bob → Data Analyst ────────────────────────────────────
INSERT INTO roadmap (id, user_id, target_job_id, target_job_title, progress_percentage)
VALUES (2, 3, 2042, 'Data Analyst', 0)
ON CONFLICT (user_id, target_job_id) DO NOTHING;

INSERT INTO roadmap_step (id, roadmap_id, title, description, order_index, status, estimated_duration)
VALUES
    (5, 2, 'SQL fondamental',
        'SELECT, JOIN, GROUP BY, fenêtres, CTEs.',
        1, 'PENDING', '3 semaines'),
    (6, 2, 'Python pour la data',
        'pandas, numpy, matplotlib, jupyter.',
        2, 'PENDING', '4 semaines'),
    (7, 2, 'Statistiques appliquées',
        'Distributions, tests d''hypothèse, régressions.',
        3, 'PENDING', '3 semaines'),
    (8, 2, 'Outils de BI',
        'Power BI ou Tableau : dashboards, KPIs.',
        4, 'PENDING', '2 semaines')
ON CONFLICT (id) DO NOTHING;

INSERT INTO resource (step_id, title, url, type) VALUES
    (5, 'PostgreSQL Tutorial',           'https://www.postgresqltutorial.com/',                 'course'),
    (5, 'Mode Analytics — SQL Tutorial', 'https://mode.com/sql-tutorial/',                      'course'),
    (6, 'pandas — Getting Started',      'https://pandas.pydata.org/docs/getting_started/',     'doc'),
    (6, 'Python Data Science Handbook',  'https://jakevdp.github.io/PythonDataScienceHandbook/','doc'),
    (7, 'Khan Academy — Statistics',     'https://www.khanacademy.org/math/statistics-probability', 'course'),
    (8, 'Microsoft Learn — Power BI',    'https://learn.microsoft.com/training/powerplatform/power-bi', 'course')
;

-- Resync les séquences (BIGSERIAL) après insertion d'IDs explicites.
SELECT setval(pg_get_serial_sequence('roadmap',      'id'), (SELECT COALESCE(MAX(id), 1) FROM roadmap));
SELECT setval(pg_get_serial_sequence('roadmap_step', 'id'), (SELECT COALESCE(MAX(id), 1) FROM roadmap_step));
SELECT setval(pg_get_serial_sequence('resource',     'id'), (SELECT COALESCE(MAX(id), 1) FROM resource));

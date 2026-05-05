CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS saved_jobs (
                                          id                  BIGSERIAL       PRIMARY KEY,
                                          uuid                UUID            NOT NULL DEFAULT uuid_generate_v4() UNIQUE,

    student_id          BIGINT          NOT NULL,

    job_external_id     VARCHAR(255)    NOT NULL,
    job_title           VARCHAR(500)    NOT NULL,
    job_company         VARCHAR(255),
    job_location        VARCHAR(255),
    job_apply_url       TEXT,

    required_skills     TEXT[],
    matched_skills      TEXT[],
    missing_skills      TEXT[],
    compatibility_score SMALLINT        CHECK (compatibility_score BETWEEN 0 AND 100),

    saved_at            TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_student_job UNIQUE (student_id, job_external_id)
    );

CREATE INDEX idx_saved_jobs_student_id
    ON saved_jobs (student_id);

CREATE INDEX idx_saved_jobs_score
    ON saved_jobs (student_id, compatibility_score DESC NULLS LAST)
    WHERE compatibility_score IS NOT NULL;
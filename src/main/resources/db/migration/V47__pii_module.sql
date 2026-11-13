-- Sentinela Cidadã PII module: privacy findings, the pii pipeline stage, Risco Cidadão snapshots and citizen alerts.

-- Privacy findings (kind PRIVACY): category, mapping/flow, the insecure output and the trace (locations only).
ALTER TABLE finding
    ADD COLUMN privacy_category VARCHAR(16),
    ADD COLUMN privacy_type     VARCHAR(8),
    ADD COLUMN privacy_sink     VARCHAR(16),
    ADD COLUMN privacy_trace    JSONB;

CREATE INDEX idx_finding_project_kind ON finding (project_id, kind);

-- Every existing repository gets the new stage enabled with the default gate; every organization the default policy.
INSERT INTO pipeline_stage_config (id, project_id, stage, enabled, tool, fail_on)
SELECT gen_random_uuid(), p.id, 'PII', TRUE, 'Semgrep', 'HIGH'
FROM project p
WHERE NOT EXISTS (SELECT 1 FROM pipeline_stage_config c WHERE c.project_id = p.id AND c.stage = 'PII');

INSERT INTO policy_stage_gate (id, organization_id, stage, fail_on, required)
SELECT gen_random_uuid(), o.id, 'PII', 'HIGH', FALSE
FROM organization o
WHERE NOT EXISTS (SELECT 1 FROM policy_stage_gate g WHERE g.organization_id = o.id AND g.stage = 'PII');

-- Risco Cidadão of a repository at the end of each completed run that ran the pii stage (dashboard trend).
CREATE TABLE citizen_risk_snapshot (
    id              UUID        PRIMARY KEY,
    organization_id UUID        NOT NULL REFERENCES organization (id) ON DELETE CASCADE,
    project_id      UUID        NOT NULL REFERENCES project (id) ON DELETE CASCADE,
    run_id          UUID        REFERENCES pipeline_run (id) ON DELETE CASCADE,
    run_number      BIGINT      NOT NULL,
    score           INT         NOT NULL CHECK (score BETWEEN 0 AND 100),
    open_flows      INT         NOT NULL DEFAULT 0,
    mapped_data     INT         NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL,
    CONSTRAINT citizen_risk_snapshot_run_key UNIQUE (run_id)
);

CREATE INDEX citizen_risk_snapshot_org_idx ON citizen_risk_snapshot (organization_id, created_at DESC);

-- AI-written citizen alerts for privacy findings, cached per organization by kind|ruleId|category|sink and language.
CREATE TABLE citizen_alert (
    id              UUID         PRIMARY KEY,
    organization_id UUID         NOT NULL REFERENCES organization (id) ON DELETE CASCADE,
    cache_key       TEXT         NOT NULL,
    language        VARCHAR(10)  NOT NULL,
    content         JSONB        NOT NULL,
    model           VARCHAR(100) NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT citizen_alert_key UNIQUE (organization_id, cache_key, language)
);

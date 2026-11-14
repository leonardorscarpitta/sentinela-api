-- Demo data for local development only: the demo organization has the AI feature, so citizen alerts and finding
-- explanations work as soon as ANTHROPIC_API_KEY is set (without a key they answer 503 ai-unavailable).
INSERT INTO organization_feature (organization_id, feature, granted_by)
VALUES ('00000000-0000-0000-0009-000000000001', 'AI_TRIAGE', 'dev seed')
ON CONFLICT DO NOTHING;

-- Demo data for local development only (loaded by the "dev" profile): Sentinela Cidadã PII module.
-- The "bairro-vivo" repository mirrors scanner/test-fixtures/citizentech-app (a fictitious CitizenTech app) with the
-- findings the pii stage reports for it, four completed runs and a decreasing Risco Cidadão trend (85 → 80 → 76 → 71).
-- All data is fictitious. No code, value or personal data is stored: findings hold rule, path, line and trace only.

-- On a fresh database V47 ran before the seeds: give the seeded repositories and organization the pii stage too.
INSERT INTO pipeline_stage_config (id, project_id, stage, enabled, tool, fail_on)
SELECT gen_random_uuid(), p.id, 'PII', TRUE, 'Semgrep', 'HIGH'
FROM project p
WHERE NOT EXISTS (SELECT 1 FROM pipeline_stage_config c WHERE c.project_id = p.id AND c.stage = 'PII');

INSERT INTO policy_stage_gate (id, organization_id, stage, fail_on, required)
SELECT gen_random_uuid(), o.id, 'PII', 'HIGH', FALSE
FROM organization o
WHERE NOT EXISTS (SELECT 1 FROM policy_stage_gate g WHERE g.organization_id = o.id AND g.stage = 'PII');

-- The repository (access pending: acme/bairro-vivo does not exist, so the runner is never asked to clone it).
INSERT INTO project (organization_id, id, name, ssh_url, branch, stack, scan_schedule, dockerfile_mode, last_scan_at,
                     created_at, access_status, stack_source)
VALUES ('00000000-0000-0000-0009-000000000001', '00000000-0000-0000-0000-000000000004', 'bairro-vivo',
        'git@github.com:acme/bairro-vivo.git', 'main', 'JAVA_SPRING', 'MANUAL', 'RECOMMENDED', '2026-11-12T21:22:00Z',
        '2026-10-20T12:00:00Z', 'PENDING', 'MANUAL');

INSERT INTO pipeline_stage_config (id, project_id, stage, enabled, tool, fail_on)
SELECT gen_random_uuid(), '00000000-0000-0000-0000-000000000004', s.* FROM (VALUES
    ('SECRETS',    TRUE,  'Gitleaks',  'LOW'),
    ('SAST',       TRUE,  'Semgrep',   'HIGH'),
    ('PII',        TRUE,  'Semgrep',   'HIGH'),
    ('SCA',        TRUE,  'Trivy',     'CRITICAL'),
    ('LICENSE',    FALSE, 'Trivy',     'NEVER'),
    ('DOCKERFILE', TRUE,  'Hadolint',  'HIGH'),
    ('IMAGE',      FALSE, 'Trivy',     'CRITICAL'),
    ('SBOM',       TRUE,  'Syft',      NULL),
    ('IAC',        FALSE, 'Trivy',     'HIGH'),
    ('DAST',       FALSE, 'OWASP ZAP', 'HIGH')
) AS s(stage, enabled, tool, fail_on);

-- Four completed runs; the pii stage blocks each one (open flows at or above "high").
INSERT INTO pipeline_run (id, number, project_id, branch, commit_sha, commit_message, trigger_type, actor, status,
                          started_at, duration_seconds, new_findings)
SELECT r.id::uuid, base.n + r.k, '00000000-0000-0000-0000-000000000004', 'main', r.sha, r.message, r.trigger, 'ana.souza',
       'FAILED', r.started::timestamptz, 120, r.new_findings
FROM (SELECT COALESCE(MAX(number), 0) AS n FROM pipeline_run) base
CROSS JOIN (VALUES
    ('00000000-0000-0000-0001-000000002001', 1, '3c9e1a7', 'Cadastro de cidadãos e triagem de saúde', 'MANUAL', '2026-10-22T21:20:00Z', 23),
    ('00000000-0000-0000-0001-000000002002', 2, 'b81f0d2', 'Remove senha dos logs de login',         'PUSH',   '2026-10-29T21:20:00Z', 0),
    ('00000000-0000-0000-0001-000000002003', 3, '5d27c44', 'DTO na consulta pública de cadastro',    'PUSH',   '2026-11-05T21:20:00Z', 0),
    ('00000000-0000-0000-0001-000000002004', 4, 'e40a9b8', 'Mascara sintomas no log da triagem',     'PUSH',   '2026-11-12T21:20:00Z', 0)
) AS r(id, k, sha, message, trigger, started, new_findings);

SELECT setval('pipeline_run_number_seq', (SELECT MAX(number) FROM pipeline_run));

INSERT INTO pipeline_run_stage (id, run_id, position, stage, tool, status, duration_seconds, findings, failed_by, log, fail_on)
SELECT gen_random_uuid(), r.id::uuid, s.position, s.stage, s.tool, s.status, s.duration,
       CASE WHEN s.stage = 'PII' THEN r.findings ELSE 0 END, CASE WHEN s.stage = 'PII' THEN 'CRITICAL' END,
       CASE WHEN s.stage = 'PII' THEN r.log ELSE s.log END, s.fail_on
FROM (VALUES
    ('00000000-0000-0000-0001-000000002001', 23, E'[Semgrep] 23 finding(s): 3 critical, 6 high, 4 medium, 10 low\n[PII] 15 mapped location(s), 8 flow(s) to insecure outputs (8 with trace)\n[PII] 1 literal match(es) dropped by check-digit validation'),
    ('00000000-0000-0000-0001-000000002002', 22, E'[Semgrep] 22 finding(s): 2 critical, 6 high, 4 medium, 10 low\n[PII] 15 mapped location(s), 7 flow(s) to insecure outputs (7 with trace)\n[PII] 1 literal match(es) dropped by check-digit validation'),
    ('00000000-0000-0000-0001-000000002003', 21, E'[Semgrep] 21 finding(s): 2 critical, 5 high, 4 medium, 10 low\n[PII] 15 mapped location(s), 6 flow(s) to insecure outputs (6 with trace)\n[PII] 1 literal match(es) dropped by check-digit validation'),
    ('00000000-0000-0000-0001-000000002004', 20, E'[Semgrep] 20 finding(s): 2 critical, 4 high, 4 medium, 10 low\n[PII] 15 mapped location(s), 5 flow(s) to insecure outputs (5 with trace)\n[PII] 1 literal match(es) dropped by check-digit validation')
) AS r(id, findings, log)
CROSS JOIN (VALUES
    (0, 'SECRETS',    'Gitleaks', 'PASSED',  14, 'LOW',      '[Gitleaks] 0 finding(s)'),
    (1, 'SAST',       'Semgrep',  'PASSED',  31, 'HIGH',     '[Semgrep] 0 finding(s)'),
    (2, 'PII',        'Semgrep',  'FAILED',  42, 'HIGH',     NULL),
    (3, 'SCA',        'Trivy',    'SKIPPED',  0, 'CRITICAL', 'Not run: the pii stage failed, so the pipeline stopped.'),
    (4, 'DOCKERFILE', 'Hadolint', 'SKIPPED',  0, 'HIGH',     'Not run: the pii stage failed, so the pipeline stopped.'),
    (5, 'SBOM',       'Syft',     'SKIPPED',  0, NULL,       'Not run: the pii stage failed, so the pipeline stopped.')
) AS s(position, stage, tool, status, duration, fail_on, log);

-- Privacy findings as reported by the pii stage. Fingerprint = sha256(lower("privacy|ruleId|path|line")), as the API.
INSERT INTO finding (id, reference, kind, fingerprint, rule_id, severity, project_id, title, description, cwe,
                     location_path, location_line, privacy_category, privacy_type, privacy_sink, privacy_trace,
                     stage, tool, status, detected_at, status_changed_at)
SELECT v.id::uuid, 'F-' || nextval('finding_reference_seq'), 'PRIVACY',
       encode(sha256(convert_to(lower('privacy|' || v.rule || '|' || v.path || '|' || v.line), 'UTF8')), 'hex'),
       v.rule, v.severity, v.project::uuid, v.title, v.description, v.cwe, v.path, v.line, v.category, v.type, v.sink,
       v.trace::jsonb, 'PII', 'Semgrep', v.status, v.detected::timestamptz, v.changed::timestamptz
FROM (VALUES
    -- Open flows (bairro-vivo)
    ('20000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000004', 'pii-flow-health-external-api', 'CRITICAL',
     'Dado de saúde enviado a serviço externo',
     'Dados de saúde do cidadão (CID-10, sintomas…) são enviados a um serviço externo. Compartilhar dado sensível de saúde com terceiros é vedado salvo hipóteses do art. 11 da LGPD; envie apenas dados agregados e anonimizados, sempre por HTTPS.',
     'CWE-201', 'src/main/java/example/bairrovivo/saude/TriagemService.java', 23, 'HEALTH', 'FLOW', 'EXTERNAL_API',
     '[{"role":"source","path":"src/main/java/example/bairrovivo/saude/TriagemService.java","line":19},{"role":"propagation","path":"src/main/java/example/bairrovivo/saude/TriagemService.java","line":18},{"role":"sink","path":"src/main/java/example/bairrovivo/saude/TriagemService.java","line":23}]',
     'OPEN', '2026-10-22T21:22:00Z', '2026-10-22T21:22:00Z'),
    ('20000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000004', 'pii-flow-credential-entity-response', 'CRITICAL',
     'Cadastro completo (com senha) exposto na resposta HTTP',
     'A entidade de cadastro inteira (com senha, documentos e contatos) é devolvida na resposta HTTP. Todo campo novo da entidade vaza automaticamente. Devolva um DTO só com os campos que a tela precisa.',
     'CWE-359', 'src/main/java/example/bairrovivo/cidadao/CidadaoController.java', 42, 'CREDENTIAL', 'FLOW', 'HTTP_RESPONSE',
     '[{"role":"source","path":"src/main/java/example/bairrovivo/cidadao/CidadaoController.java","line":42},{"role":"sink","path":"src/main/java/example/bairrovivo/cidadao/CidadaoController.java","line":42}]',
     'OPEN', '2026-10-22T21:22:00Z', '2026-10-22T21:22:00Z'),
    ('20000000-0000-0000-0000-000000000003', '00000000-0000-0000-0000-000000000004', 'pii-flow-identification-log', 'HIGH',
     'Documento do cidadão gravado em log',
     'Um documento de identificação do cidadão (CPF, RG, CNS…) é gravado em log sem máscara. Logs são lidos por equipes, ferramentas e fornecedores e guardados por muito tempo. Mascare o valor (PiiMasker) ou não o registre.',
     'CWE-532', 'src/main/java/example/bairrovivo/cidadao/CidadaoController.java', 32, 'IDENTIFICATION', 'FLOW', 'LOG',
     '[{"role":"source","path":"src/main/java/example/bairrovivo/cidadao/CidadaoController.java","line":30},{"role":"sink","path":"src/main/java/example/bairrovivo/cidadao/CidadaoController.java","line":32}]',
     'TRIAGED', '2026-10-22T21:22:00Z', '2026-10-23T14:00:00Z'),
    ('20000000-0000-0000-0000-000000000004', '00000000-0000-0000-0000-000000000004', 'pii-flow-identification-file', 'HIGH',
     'Documento do cidadão gravado em arquivo',
     'Um documento de identificação do cidadão é gravado em arquivo sem proteção (ex.: /tmp ou exportações). Arquivos soltos escapam do controle de acesso e da retenção. Use armazenamento protegido e criptografado.',
     'CWE-312', 'src/main/java/example/bairrovivo/saude/TriagemService.java', 27, 'IDENTIFICATION', 'FLOW', 'FILE',
     '[{"role":"source","path":"src/main/java/example/bairrovivo/saude/TriagemService.java","line":25},{"role":"sink","path":"src/main/java/example/bairrovivo/saude/TriagemService.java","line":27}]',
     'OPEN', '2026-10-22T21:22:00Z', '2026-10-22T21:22:00Z'),
    ('20000000-0000-0000-0000-000000000005', '00000000-0000-0000-0000-000000000004', 'pii-flow-contact-exception', 'HIGH',
     'Contato do cidadão em mensagem de erro',
     'Um contato do cidadão (telefone, e-mail…) vai para a mensagem de uma exceção, que acaba em logs e telas de erro. Descreva o erro sem o dado.',
     'CWE-209', 'src/main/java/example/bairrovivo/zeladoria/ChamadoService.java', 12, 'CONTACT', 'FLOW', 'EXCEPTION',
     '[{"role":"source","path":"src/main/java/example/bairrovivo/zeladoria/ChamadoService.java","line":9},{"role":"sink","path":"src/main/java/example/bairrovivo/zeladoria/ChamadoService.java","line":12}]',
     'OPEN', '2026-10-22T21:22:00Z', '2026-10-22T21:22:00Z'),
    -- Flows fixed in later runs (they explain the falling trend)
    ('20000000-0000-0000-0000-000000000006', '00000000-0000-0000-0000-000000000004', 'pii-flow-credential-log', 'CRITICAL',
     'Senha do cidadão gravada em log',
     'A senha do cidadão é gravada em log. Quem lê os logs pode entrar na conta dele (e em outros serviços, se ele repetir a senha). Nunca registre senhas.',
     'CWE-532', 'src/main/java/example/bairrovivo/auth/LoginController.java', 34, 'CREDENTIAL', 'FLOW', 'LOG',
     '[{"role":"source","path":"src/main/java/example/bairrovivo/auth/LoginController.java","line":33},{"role":"sink","path":"src/main/java/example/bairrovivo/auth/LoginController.java","line":34}]',
     'FIXED', '2026-10-22T21:22:00Z', '2026-10-29T21:22:00Z'),
    ('20000000-0000-0000-0000-000000000007', '00000000-0000-0000-0000-000000000004', 'pii-flow-identification-http-response', 'HIGH',
     'Documento do cidadão exposto na resposta HTTP',
     'Um documento de identificação do cidadão é devolvido na resposta HTTP sem máscara. Qualquer cliente da API (ou quem intercepte a resposta) passa a ter o dado. Devolva apenas o necessário, mascarado.',
     'CWE-359', 'src/main/java/example/bairrovivo/cidadao/ConsultaPublicaController.java', 28, 'IDENTIFICATION', 'FLOW', 'HTTP_RESPONSE',
     '[{"role":"source","path":"src/main/java/example/bairrovivo/cidadao/ConsultaPublicaController.java","line":25},{"role":"sink","path":"src/main/java/example/bairrovivo/cidadao/ConsultaPublicaController.java","line":28}]',
     'FIXED', '2026-10-22T21:22:00Z', '2026-11-05T21:22:00Z'),
    ('20000000-0000-0000-0000-000000000008', '00000000-0000-0000-0000-000000000004', 'pii-flow-health-log', 'HIGH',
     'Dado de saúde gravado em log',
     'Dados de saúde do cidadão (CID-10, sintomas, diagnóstico, CNS…) são gravados em log. Dado de saúde é dado sensível pela LGPD (art. 11): não registre, ou anonimize antes.',
     'CWE-532', 'src/main/java/example/bairrovivo/saude/TriagemController.java', 29, 'HEALTH', 'FLOW', 'LOG',
     '[{"role":"source","path":"src/main/java/example/bairrovivo/saude/TriagemController.java","line":27},{"role":"sink","path":"src/main/java/example/bairrovivo/saude/TriagemController.java","line":29}]',
     'FIXED', '2026-10-22T21:22:00Z', '2026-11-12T21:22:00Z'),
    -- Mapped personal data (bairro-vivo)
    ('20000000-0000-0000-0000-000000000011', '00000000-0000-0000-0000-000000000004', 'pii-map-identification-field', 'LOW',
     'Documento do cidadão armazenado', 'Campo com documento de identificação do cidadão (CPF, RG, CNS, CNH, NIS…). Registre-o no inventário de dados pessoais e garanta máscara em logs e respostas.',
     'CWE-359', 'src/main/java/example/bairrovivo/cidadao/Cidadao.java', 16, 'IDENTIFICATION', 'MAPPING', NULL, NULL,
     'OPEN', '2026-10-22T21:22:00Z', '2026-10-22T21:22:00Z'),
    ('20000000-0000-0000-0000-000000000012', '00000000-0000-0000-0000-000000000004', 'pii-map-contact-field', 'LOW',
     'Contato do cidadão armazenado', 'Campo com contato do cidadão (telefone, e-mail, endereço). Registre no inventário e mascare em logs e telas públicas.',
     'CWE-359', 'src/main/java/example/bairrovivo/cidadao/Cidadao.java', 18, 'CONTACT', 'MAPPING', NULL, NULL,
     'OPEN', '2026-10-22T21:22:00Z', '2026-10-22T21:22:00Z'),
    ('20000000-0000-0000-0000-000000000013', '00000000-0000-0000-0000-000000000004', 'pii-map-contact-field', 'LOW',
     'Contato do cidadão armazenado', 'Campo com contato do cidadão (telefone, e-mail, endereço). Registre no inventário e mascare em logs e telas públicas.',
     'CWE-359', 'src/main/java/example/bairrovivo/cidadao/Cidadao.java', 20, 'CONTACT', 'MAPPING', NULL, NULL,
     'OPEN', '2026-10-22T21:22:00Z', '2026-10-22T21:22:00Z'),
    ('20000000-0000-0000-0000-000000000014', '00000000-0000-0000-0000-000000000004', 'pii-map-credential-field', 'LOW',
     'Senha armazenada', 'Campo com senha do cidadão. Guarde somente o hash (bcrypt/argon2), nunca o texto, e exclua o campo de qualquer resposta ou log.',
     'CWE-359', 'src/main/java/example/bairrovivo/cidadao/Cidadao.java', 23, 'CREDENTIAL', 'MAPPING', NULL, NULL,
     'OPEN', '2026-10-22T21:22:00Z', '2026-10-22T21:22:00Z'),
    ('20000000-0000-0000-0000-000000000015', '00000000-0000-0000-0000-000000000004', 'pii-map-identification-field', 'LOW',
     'Documento do cidadão armazenado', 'Campo com documento de identificação do cidadão (CPF, RG, CNS, CNH, NIS…). Registre-o no inventário de dados pessoais e garanta máscara em logs e respostas.',
     'CWE-359', 'src/main/java/example/bairrovivo/saude/Triagem.java', 8, 'IDENTIFICATION', 'MAPPING', NULL, NULL,
     'OPEN', '2026-10-22T21:22:00Z', '2026-10-22T21:22:00Z'),
    ('20000000-0000-0000-0000-000000000016', '00000000-0000-0000-0000-000000000004', 'pii-map-health-field', 'MEDIUM',
     'Dado de saúde armazenado', 'Campo com dado de saúde do cidadão (CID-10, sintomas, diagnóstico, laudo…). Dado sensível pela LGPD (art. 11): acesso restrito, criptografia em repouso e nunca em logs.',
     'CWE-359', 'src/main/java/example/bairrovivo/saude/Triagem.java', 10, 'HEALTH', 'MAPPING', NULL, NULL,
     'OPEN', '2026-10-22T21:22:00Z', '2026-10-22T21:22:00Z'),
    ('20000000-0000-0000-0000-000000000017', '00000000-0000-0000-0000-000000000004', 'pii-map-health-field', 'MEDIUM',
     'Dado de saúde armazenado', 'Campo com dado de saúde do cidadão (CID-10, sintomas, diagnóstico, laudo…). Dado sensível pela LGPD (art. 11): acesso restrito, criptografia em repouso e nunca em logs.',
     'CWE-359', 'src/main/java/example/bairrovivo/saude/Triagem.java', 12, 'HEALTH', 'MAPPING', NULL, NULL,
     'OPEN', '2026-10-22T21:22:00Z', '2026-10-22T21:22:00Z'),
    ('20000000-0000-0000-0000-000000000018', '00000000-0000-0000-0000-000000000004', 'pii-map-contact-field', 'LOW',
     'Contato do cidadão armazenado', 'Campo com contato do cidadão (telefone, e-mail, endereço). Registre no inventário e mascare em logs e telas públicas.',
     'CWE-359', 'src/main/java/example/bairrovivo/zeladoria/Chamado.java', 10, 'CONTACT', 'MAPPING', NULL, NULL,
     'OPEN', '2026-10-22T21:22:00Z', '2026-10-22T21:22:00Z'),
    ('20000000-0000-0000-0000-000000000019', '00000000-0000-0000-0000-000000000004', 'pii-map-contact-field', 'LOW',
     'Contato do cidadão armazenado', 'Campo com contato do cidadão (telefone, e-mail, endereço). Registre no inventário e mascare em logs e telas públicas.',
     'CWE-359', 'src/main/java/example/bairrovivo/zeladoria/Chamado.java', 12, 'CONTACT', 'MAPPING', NULL, NULL,
     'OPEN', '2026-10-22T21:22:00Z', '2026-10-22T21:22:00Z'),
    ('20000000-0000-0000-0000-000000000020', '00000000-0000-0000-0000-000000000004', 'pii-config-credential', 'HIGH',
     'Senha em arquivo de configuração', 'Senha ou segredo escrito em arquivo de configuração. Quem tem acesso ao repositório (ou a um backup dele) chega ao banco com os dados dos cidadãos. Leia o valor de variável de ambiente ou cofre de segredos e troque a senha.',
     'CWE-260', 'src/main/resources/application.properties', 5, 'CREDENTIAL', 'MAPPING', NULL, NULL,
     'OPEN', '2026-10-22T21:22:00Z', '2026-10-22T21:22:00Z'),
    ('20000000-0000-0000-0000-000000000021', '00000000-0000-0000-0000-000000000004', 'pii-literal-cpf', 'MEDIUM',
     'CPF escrito no repositório', 'Número de CPF válido escrito no repositório (código, SQL ou fixture). Se for de uma pessoa real, é um vazamento já consumado: remova do histórico e use dados sintéticos gerados em tempo de teste.',
     'CWE-359', 'src/main/resources/data.sql', 4, 'IDENTIFICATION', 'MAPPING', NULL, NULL,
     'OPEN', '2026-10-22T21:22:00Z', '2026-10-22T21:22:00Z'),
    ('20000000-0000-0000-0000-000000000022', '00000000-0000-0000-0000-000000000004', 'pii-literal-email', 'LOW',
     'E-mail escrito no repositório', 'Endereço de e-mail pessoal escrito no repositório (dados de teste ou seeds). Prefira domínios reservados (example.com) em dados de teste.',
     'CWE-359', 'src/main/resources/data.sql', 4, 'CONTACT', 'MAPPING', NULL, NULL,
     'OPEN', '2026-10-22T21:22:00Z', '2026-10-22T21:22:00Z'),
    ('20000000-0000-0000-0000-000000000023', '00000000-0000-0000-0000-000000000004', 'pii-literal-phone-br', 'LOW',
     'Celular escrito no repositório', 'Número de celular brasileiro escrito no repositório. Use números fictícios reservados em dados de teste.',
     'CWE-359', 'src/main/resources/data.sql', 4, 'CONTACT', 'MAPPING', NULL, NULL,
     'OPEN', '2026-10-22T21:22:00Z', '2026-10-22T21:22:00Z'),
    ('20000000-0000-0000-0000-000000000024', '00000000-0000-0000-0000-000000000004', 'pii-literal-phone-br', 'LOW',
     'Celular escrito no repositório', 'Número de celular brasileiro escrito no repositório. Use números fictícios reservados em dados de teste.',
     'CWE-359', 'src/main/resources/data.sql', 8, 'CONTACT', 'MAPPING', NULL, NULL,
     'OPEN', '2026-10-22T21:22:00Z', '2026-10-22T21:22:00Z'),
    ('20000000-0000-0000-0000-000000000025', '00000000-0000-0000-0000-000000000004', 'pii-literal-cns', 'MEDIUM',
     'Cartão SUS (CNS) escrito no repositório', 'Número de Cartão Nacional de Saúde (CNS) válido escrito no repositório. É dado de saúde (LGPD art. 11): remova e use dados sintéticos.',
     'CWE-359', 'src/main/resources/data.sql', 11, 'HEALTH', 'MAPPING', NULL, NULL,
     'OPEN', '2026-10-22T21:22:00Z', '2026-10-22T21:22:00Z'),
    -- A second repository with personal data, so the organization view aggregates more than one
    ('20000000-0000-0000-0000-000000000031', '00000000-0000-0000-0000-000000000002', 'pii-flow-financial-log', 'HIGH',
     'Dado financeiro gravado em log', 'Dados financeiros do cidadão (cartão, conta, PIX, renda) são gravados em log. Não registre; mascare.',
     'CWE-532', 'src/main/java/com/acme/payments/pix/PixRefundService.java', 58, 'FINANCIAL', 'FLOW', 'LOG',
     '[{"role":"source","path":"src/main/java/com/acme/payments/pix/PixRefundService.java","line":52},{"role":"propagation","path":"src/main/java/com/acme/payments/pix/PixRefundService.java","line":55},{"role":"sink","path":"src/main/java/com/acme/payments/pix/PixRefundService.java","line":58}]',
     'OPEN', '2026-11-10T09:00:00Z', '2026-11-10T09:00:00Z'),
    ('20000000-0000-0000-0000-000000000032', '00000000-0000-0000-0000-000000000002', 'pii-map-financial-field', 'LOW',
     'Dado financeiro armazenado', 'Campo com dado financeiro do cidadão (cartão, conta, PIX, renda). Prefira tokens do provedor de pagamento e mascare sempre.',
     'CWE-359', 'src/main/java/com/acme/payments/pix/PixKey.java', 14, 'FINANCIAL', 'MAPPING', NULL, NULL,
     'OPEN', '2026-11-10T09:00:00Z', '2026-11-10T09:00:00Z')
) AS v(id, project, rule, severity, title, description, cwe, path, line, category, type, sink, trace, status, detected, changed);

INSERT INTO finding_event (id, finding_id, type, status, note, occurred_at)
SELECT gen_random_uuid(), f.id, 'DETECTED', 'OPEN', NULL, f.detected_at
FROM finding f WHERE f.id::text LIKE '20000000-0000-0000-0000-0000000000%';

INSERT INTO finding_event (id, finding_id, type, status, note, occurred_at)
SELECT gen_random_uuid(), f.id, 'STATUS_CHANGED', f.status,
       CASE f.status WHEN 'FIXED' THEN 'No longer detected by Semgrep' ELSE 'Confirmed: CPF goes to the log in full' END,
       f.status_changed_at
FROM finding f WHERE f.id::text LIKE '20000000-0000-0000-0000-0000000000%' AND f.status IN ('FIXED', 'TRIAGED');

-- Risco Cidadão at the end of each run (the score formula applied to the privacy findings open at that time).
INSERT INTO citizen_risk_snapshot (id, organization_id, project_id, run_id, run_number, score, open_flows, mapped_data, created_at)
SELECT gen_random_uuid(), '00000000-0000-0000-0009-000000000001', '00000000-0000-0000-0000-000000000004', r.id, r.number,
       s.score, s.flows, 15, r.started_at + INTERVAL '120 seconds'
FROM pipeline_run r
JOIN (VALUES
    ('00000000-0000-0000-0001-000000002001', 85, 8),
    ('00000000-0000-0000-0001-000000002002', 80, 7),
    ('00000000-0000-0000-0001-000000002003', 76, 6),
    ('00000000-0000-0000-0001-000000002004', 71, 5)
) AS s(run_id, score, flows) ON r.id = s.run_id::uuid;

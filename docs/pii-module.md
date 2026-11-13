# Sentinela Cidadã — PII module (backend ⇄ frontend contract, as implemented)

## Deviations and clarifications (read first)

The implementation follows the original contract; these points refine or deviate from it:

1. **`GET /findings/{id}/citizen-alert` on a non-privacy finding** answers `400 not-privacy-finding` (like the POST),
   not `404`. `404` means "privacy finding without a cached alert" (or an unknown finding).
2. **`byCategory`** always lists the five categories (zeros included), sorted by `weight` descending. `weight` is the
   category's share of Σ (before saturation), rounded to one decimal.
3. **`citizenAlerts`** = open privacy findings in scope whose citizen alert is already cached (any language).
   **`repositories`** = repositories in scope with open privacy findings.
4. **`trend` for the organization** = the last 12 snapshots across its connected repositories (each point names its
   repository); with `?repository=` only that repository's snapshots. Unknown `repository` → `404`.
5. **Weights** are serialized as JSON numbers with decimals (`10.0`, `60.0`).
6. **Daily AI limit** for citizen alerts reuses the `AI_INSIGHTS_PER_DAY` value (default 50) but has its own counter
   (`organization_ai_usage.kind = 'citizenAlert'`), so alerts and insights do not consume each other's budget.
7. **Model**: the configured `ANTHROPIC_MODEL` (default `claude-haiku-5-5`); `model` in the answer is whatever wrote it.
8. **Mappings can also fail the gate**: mapping severities are `low`/`medium`, except a credential written in a
   configuration file (`pii-config-credential`), which is `high` and therefore also fails `failOn: high`. The
   `verdict` of the Risco Cidadão still only looks at open flows.
9. **Trace**: consecutive steps on the same line are merged (`String doc = c.getCpf();` → one `source` step); for the
   account-entity rule the source is the entity variable at the sink line. When the trace cannot be recovered the
   runner sends a single `sink` step (never an empty trace for a flow).
10. The dev seed enables `aiTriage` for the demo organization (`acme`), so without `ANTHROPIC_API_KEY`
    `POST …/citizen-alert` answers `503 ai-unavailable` (with the feature off it would be `403 feature-not-enabled`).

---

Product name everywhere users see it: **Sentinela Cidadã**; technical identifiers use `sentinela`
(`br.com.sentinelacidada`, `sentinela.*`, Keycloak realm/client ids, `X-Sentinela-Organization`). Everything below is
the PII module, on top of the rest of the API documented in `README.md`.

Wire style follows the existing API: JSON camelCase, enum values lowercase/camelCase strings, errors as RFC 7807
ProblemDetail with `code`.

## 1. Pipeline stage `pii`

- `Stage.PII` — phase `source`, tools `["Semgrep"]`, enabled by default, default `failOn` = `high`, available; runs
  right after `sast`.
- Shows up in `GET /api/v1/pipeline/stages`, in repository pipelines and in the organization policies (`GET /policies`).
  Migration `V47__pii_module.sql` adds it (enabled, `failOn: high`) to every existing repository and the gate
  (`high`, not required) to every organization; new organizations get it from `PolicyProvisioning`.
- Runner (`scanner/worker/tools/pii.py`): Semgrep CE with the rules in `scanner/rules/pii/` (never the registry). The
  SAST stage loads only the top-level files of `scanner/rules/`, so the two stages never mix.
  - **mapping** (`mapping-java.yml`, `mapping-literals.yml`) — where personal data lives: literal values (CPF, CNS,
    payment card, CID-10 next to a `cid` label, e-mail, celular, credentials in configuration files) and Java fields /
    record components named after personal data (cpf, rg, cns, cid, diagnostico, sintomas, laudo, senha/password,
    telefone, email, endereco, cartao, chavePix…), restricted to value types. Literal rules carry
    `metadata.sentinela.validator: cpf | cns | luhn`; the runner reads the matched text **locally**, checks the digits
    and drops invalid matches (and the public test card numbers). The value is never sent.
  - **flow** (`flow-java.yml`, `flow-python.yml`, `flow-javascript.yml`) — Semgrep `mode: taint`, one rule per
    category × sink (`pii-flow-<category>-<sink>`, `-py`/`-js` suffix for Python and JS/TS) plus
    `pii-flow-credential-entity-response` (an account entity — `Cidadao`, `Usuario`, `Paciente`… — returned whole).
    Sources = PII getters/accessors/attributes, Spring request parameters, Flask/Django request keys, Express
    `req.*` fields; sinks = loggers and stdout, HTTP responses, outbound HTTP clients, file writes, exception messages;
    sanitizers = `PiiMasker.*`, `mask*(…)`, `hash*(…)`, `anonymi[sz]e*(…)`, `redact*`, `encrypt*`.
  - **Trace**: Semgrep CE's CLI no longer includes `dataflow_trace` in JSON output, so for each file with a flow
    finding the runner re-runs the engine (`semgrep-core`, same rules, anchors expanded at image build) on that file
    and copies the trace **locations** (source, intermediate variables, sink). Code excerpts are discarded.
- Gate: flow findings are `high`/`critical`, so with the default `failOn: high` the stage fails and the run is blocked —
  "bloquear vazamentos em saídas inseguras".

## 2. Finding kind `privacy`

`kind: "privacy"` (`FindingKind.PRIVACY`). Runner payload (in addition to the usual fields; `location` required):

```json
{ "kind": "privacy", "ruleId": "pii-flow-health-external-api", "title": "…", "description": "…",
  "severity": "critical", "cwe": "CWE-201",
  "location": { "path": "src/main/java/…/TriagemService.java", "line": 23 },
  "privacy": {
    "category": "identification | health | credential | contact | financial",
    "type": "mapping | flow",
    "sink": "log | httpResponse | externalApi | file | exception | null",
    "trace": [ { "role": "source | propagation | sink", "path": "…", "line": 19 } ]
  } }
```

- `sink` is null and `trace` empty for `mapping`. Paths relative (validated like `location.path`), lines only — no
  code, no values. At most 50 steps accepted, 20 stored.
- Validation: a `privacy` finding without `privacy.category`/`privacy.type`, or a flow without `sink`, is rejected
  (`400`); `privacy` is ignored for other kinds.
- Stored on the finding (`privacy_category`, `privacy_type`, `privacy_sink`, `privacy_trace` JSONB — V47) and
  returned on `GET /api/v1/findings` items and the detail (`finding.privacy`) as `privacy: {category, type, sink,
  trace}` (null for other kinds). `GET /findings?kind=privacy` filters (also `/findings/export`).
- Fingerprint for privacy findings: `sha256(lower("privacy|ruleId|path|line"))`, line = the match (mapping) or the sink
  (flow).

## 3. Risco Cidadão

`GET /api/v1/dashboard/citizen-risk?repository=<name>` (optional; default whole organization), `viewFindings`.

```json
{ "score": 76, "level": "critical", "verdict": "blocked",
  "openFlows": 6, "mappedData": 16, "citizenAlerts": 0, "repositories": 2,
  "byCategory": [ { "category": "health", "weight": 28.1, "flows": 1, "mapped": 3 }, … ],
  "trend": [ { "runNumber": 1288, "repository": "bairro-vivo", "date": "2026-11-12T21:22:00Z", "score": 71 } ],
  "weights": { "severity": {"critical":10.0,"high":6.0,"medium":3.0,"low":1.0},
               "category": {"health":1.5,"credential":1.4,"financial":1.3,"identification":1.2,"contact":0.8},
               "sink": {"externalApi":1.6,"httpResponse":1.4,"log":1.2,"exception":1.1,"file":1.0},
               "latentFactor": 0.3, "saturation": 60.0 } }
```

- `level`: critical ≥ 75, high ≥ 50, moderate ≥ 25, low. `verdict`: `blocked` when any open privacy flow exists, else
  `approved`.
- Open = status `open` or `triaged`; removed repositories excluded (same as the other dashboard counters).
- flow weight = severity × category × sink; mapping weight = severity × category × latentFactor;
  `score = round(100 × (1 − e^(−Σ/saturation)))`.
- `trend`: one point per completed run that scanned the `pii` stage (stage passed, warning or failed by the gate), last
  12, oldest first: the repository's score after that run, stored when the run completes (`citizen_risk_snapshot`,
  V47; written after the completion commits, in its own transaction, once per run).

## 4. Citizen alerts (AI translation)

Only for `privacy` findings. Same gating as finding insights (`aiTriage` feature + `ANTHROPIC_API_KEY`, daily limit,
`ClaudeClient` with JSON-schema structured output, effort low, no retries), cached per organization and language by
`privacy|ruleId|category|sink` (table `citizen_alert`, V47).

- `GET  /api/v1/findings/{id}/citizen-alert` (`viewFindings`) → cached alert (organization language first, else any) or
  `404`; `400 not-privacy-finding` for other kinds.
- `POST /api/v1/findings/{id}/citizen-alert` (`triage`) → cached or generated. Errors: `403 feature-not-enabled`,
  `400 not-privacy-finding`, `503 ai-unavailable`, `503 ai-busy`, `429 ai-limit`, `502 ai-failed`.

```json
{ "title": "Seu CPF fica guardado nos registros internos do sistema",
  "whatHappens": "…", "whoIsAffected": "…", "citizenGuidance": "…", "technicalFix": "…",
  "severity": "high", "language": "pt-BR", "model": "claude-haiku-5-5", "createdAt": "…" }
```

Answer validation: every field required, plain text (control characters and markdown stripped), at most 120 / 500 /
300 / 450 / 300 characters; otherwise `502 ai-failed` and nothing is cached. `severity` is the finding's (not the AI's).

Data sent to Claude (built only in `CitizenAlertPayload`): `lang`, `ruleId`, `category`, `type`, `sink`, `severity`,
`cwe`, the rule's title and the repository stack. Never code, paths, lines, values, names or e-mails. The system
instruction is recorded in `prompts.md`.

## 5. Unchanged API the frontend also uses

Auth (Keycloak OIDC Authorization Code + PKCE, client `sentinela-web`, or `AUTH_ENABLED=false` in dev), `GET /me`,
`GET /me/organizations`, header `X-Sentinela-Organization: <slug>`, repositories (`GET/POST /repositories`,
`/repositories/{name}/access`, `/access/key`, `/access/checks`, `/scans`), `GET /pipeline-runs`,
`GET /pipeline-runs/{number}`, `GET /findings`, `GET /findings/{id}`, `POST /findings/{id}/status`,
`GET /dashboard/summary` (`byKind` now includes `privacy`), `GET /usage`. Read the controllers for exact shapes.

## Dev seed

`V1009__seed_citizentech_privacy.sql` (dev profile): repository `bairro-vivo` in `acme` mirroring the fixture
`scanner/test-fixtures/citizentech-app` (20 open/triaged privacy findings + 3 fixed flows), four completed runs whose
`pii` stage blocked the pipeline and snapshots 85 → 80 → 76 → 71; two privacy findings in `payments-api`.
`V1010__seed_demo_ai_feature.sql` enables `aiTriage` for `acme`.

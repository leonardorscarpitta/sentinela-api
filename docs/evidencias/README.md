# Evidências — Taint Analysis do módulo PII contra o protótipo "Bairro Vivo"

`pii-scan-bairro-vivo.json` é a saída normalizada (a mesma que o runner envia à API) do estágio `pii` executado em
modo local sobre o protótipo CitizenTech fictício `scanner/test-fixtures/citizentech-app/` ("Bairro Vivo":
cadastro de cidadãos, zeladoria e triagem de saúde em Spring Boot). Todos os dados do protótipo são fictícios.

```sh
docker build -t sentinela/runner:dev scanner
docker run --rm --read-only --cap-drop ALL --tmpfs /tmp --tmpfs /workspace:uid=10001,gid=10001 \
  -v "$PWD/scanner/test-fixtures/citizentech-app:/fixture:ro" \
  sentinela/runner:dev scan --dir /fixture --stages pii
```

Resultado: **20 achados** — 5 fluxos (taint) com rastro completo, 15 mapeamentos — e **1 literal descartado** pela
validação de dígito verificador. O arquivo foi conferido antes de ser versionado: contém somente regra, caminho
relativo, linha, categoria, saída insegura e rastro. **Nenhum valor** (CPF, CNS, senha, telefone, e-mail) e **nenhum
trecho de código** sai do runner.

## O que cada achado prova

### Fluxos de dados pessoais até saídas inseguras (Taint Analysis)

| Regra | Fonte → saída | Rastro (linhas) | Severidade |
| --- | --- | --- | --- |
| `pii-flow-identification-log` | `salvo.getCpf()` → `log.info(...)` em `CidadaoController` | fonte 30 → sink 32 | high |
| `pii-flow-credential-entity-response` | entidade `Cidadao` (com `senha`) → `ResponseEntity.ok(...)` | 42 → 42 | critical |
| `pii-flow-health-external-api` | `getCid10()`/`getSintomas()` → `Map.of(...)` → `RestTemplate.postForObject("http://analytics.parceiro.example/…")` | fonte 19 → propagação 18 (`evento`) → sink 23 | critical |
| `pii-flow-identification-file` | `getCns()` → `Files.writeString(Path.of("/tmp/…"))` | 25 → 27 | high |
| `pii-flow-contact-exception` | `getTelefoneContato()` → `throw new IllegalArgumentException("…" + contato)` | 9 → 12 | high |

- **Fontes**: getters/acessores e parâmetros `@RequestParam`/`@PathVariable` cujo nome indica dado pessoal; para a
  entidade de cadastro, o próprio objeto (`Cidadao`, `Usuario`, `Paciente`…).
- **Propagação**: o rastro mostra a variável intermediária (`evento`, linha 18) pela qual os dados de saúde passam
  antes de sair para o parceiro externo. Quando a variável intermediária está na mesma linha da fonte
  (`String documento = salvo.getCpf();`), o passo é unido à fonte.
- **Bloqueio**: fluxos são `high`/`critical`; com o gate padrão do estágio (`failOn: high`) o estágio falha e a
  pipeline para — é o requisito "bloquear vazamentos em saídas inseguras". Verificado também ponta a ponta: este JSON
  enviado pela API interna do runner (`PUT /internal/v1/jobs/{runId}/stages/pii`) resultou no estágio `pii` = `failed`
  (`failedBy: high`), nos achados deduplicados contra os do seed e num novo ponto da tendência do Risco Cidadão.

### Fluxo sanitizado — ausente (prova de baixo falso positivo)

`CidadaoController.java:34` grava o mesmo CPF em log, mas mascarado por `PiiMasker.cpf(...)`. `PiiMasker.*` (e
`mask*`, `hash*`, `anonymize*`, `redact*`, `encrypt*`) são sanitizadores das regras de taint, então **não há achado
na linha 34**. Da mesma forma, `ResponseEntity.ok(salvo.getId())` (linha 35) não é reportado: ler um campo não
sensível da entidade (getter) não é tratado como devolver a entidade inteira.

### Mapeamento de dados pessoais

- Campos com nome de dado pessoal e tipo de valor (String, números, datas, listas): `Cidadao.cpf/email/telefone/senha`,
  `Triagem.cns/cid10/sintomas`, `Chamado.endereco/telefoneContato`. Serviços, repositórios e configurações não entram
  (o tipo do campo é verificado), nem variáveis locais.
- `application.properties:5` — senha do banco escrita no arquivo (`pii-config-credential`, high). O valor não aparece
  na evidência.
- `data.sql:4` — CPF `529.982.247-25` (**dígitos verificadores válidos**), e-mail e celular fictícios.
- `data.sql:11` — CNS `700 0012 3456 7894` (**soma ponderada módulo 11 válida**).

### Literal com dígito verificador inválido — descartado

`data.sql:8` contém o CPF `123.456.789-00`, que casa com o padrão mas tem dígitos verificadores inválidos. O runner lê
o trecho localmente, valida (módulo 11) e **descarta** o achado — por isso não há `pii-literal-cpf` na linha 8, e o
log registra "1 literal match(es) dropped by check-digit validation". (O celular da mesma linha continua reportado:
não há dígito verificador para telefones.)

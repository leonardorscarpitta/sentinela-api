# Scanner test fixtures

`vulnerable-app/` is intentionally insecure test input for the runner's local mode. It is never built or executed.
Run `./make-secret-fixture.sh` first to generate a fake credential for the secrets stage (git-ignored).

`citizentech-app/` ("Bairro Vivo") is a fictitious CitizenTech Spring Boot app for the `pii` stage: five deliberate
personal-data leaks (CPF → log, CID-10/sintomas → external API, entity with `senha` → HTTP response, telefone →
exception message, CNS → file in `/tmp`), one masked CPF log that must not be reported, a fake database password and
fake CPF/CNS values (one CPF with invalid check digits, which the validator must drop). Never built or executed; all
data is fictitious. Expected result: `docs/evidencias/`.

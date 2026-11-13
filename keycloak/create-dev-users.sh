#!/usr/bin/env sh
# Creates development users in the sentinela realm (random passwords, e-mail already verified).
# Runs Keycloak's admin CLI inside the running container; nothing is installed on the host.
set -eu
cd "$(dirname "$0")/.."
mkdir -p keycloak/secrets
OUT=keycloak/secrets/dev-credentials.txt
. ./.env

# docker compose refuses to run without KEYCLOAK_ADMIN_API_CLIENT_SECRET, so make sure it exists first.
secret="${KEYCLOAK_ADMIN_API_CLIENT_SECRET:-}"
if [ -z "$secret" ]; then
  secret="$(openssl rand -hex 32)"
  sed -i '/^KEYCLOAK_ADMIN_API_CLIENT_SECRET=$/d' .env
  printf '\nKEYCLOAK_ADMIN_API_CLIENT_SECRET=%s\n' "$secret" >> .env
  echo "Wrote KEYCLOAK_ADMIN_API_CLIENT_SECRET to .env — recreate Keycloak and the API: docker compose up -d keycloak api"
fi

kcadm() {
  docker compose exec -T keycloak /opt/keycloak/bin/kcadm.sh "$@" </dev/null
}

# For values that must not appear in process arguments: JSON on stdin (-f -).
kcadm_stdin() {
  docker compose exec -T keycloak /opt/keycloak/bin/kcadm.sh "$@"
}

kcadm config credentials --server http://localhost:8080 --realm master \
  --user "$KEYCLOAK_ADMIN_USERNAME" --password "$KEYCLOAK_ADMIN_PASSWORD" >/dev/null

: > "$OUT"
while IFS=: read -r username first last email; do
  password="$(openssl rand -base64 18 | tr -d '/+=' | cut -c1-20)"
  if ! kcadm get users -r sentinela -q "username=$username" --fields id | grep -q '"id"'; then
    kcadm create users -r sentinela -s "username=$username" -s "firstName=$first" -s "lastName=$last" \
      -s "email=$email" -s emailVerified=true -s enabled=true >/dev/null
  fi
  kcadm set-password -r sentinela --username "$username" --new-password "$password" >/dev/null
  printf '%s  %s\n' "$username" "$password" >> "$OUT"
done <<USERS
ana.souza:Ana:Souza:ana.souza@acme.example
carla.mendes:Carla:Mendes:carla.mendes@acme.example
daniel.rocha:Daniel:Rocha:daniel.rocha@acme.example
admin.sentinela:Sentinela:Admin:admin@sentinela.example
USERS

# Platform administrator (the role is in the realm import; created here for realms imported before it existed).
kcadm get roles/sentinela-admin -r sentinela >/dev/null 2>&1 || kcadm create roles -r sentinela -s name=sentinela-admin >/dev/null
kcadm add-roles -r sentinela --uusername admin.sentinela --rolename sentinela-admin

# Service-account client for user management (in the realm import; created here for realms imported before it).
# Always (re)sets the secret from .env, which also fixes a realm imported with an empty secret.
cid="$(kcadm get clients -r sentinela -q clientId=sentinela-admin-api --fields id --format csv --noquotes)"
if [ -z "$cid" ]; then
  printf '{"secret":"%s"}' "$secret" | kcadm_stdin create clients -r sentinela -f - \
    -s clientId=sentinela-admin-api -s 'name=Sentinela API (user administration)' \
    -s enabled=true -s publicClient=false -s clientAuthenticatorType=client-secret -s serviceAccountsEnabled=true \
    -s standardFlowEnabled=false -s implicitFlowEnabled=false -s directAccessGrantsEnabled=false \
    -s frontchannelLogout=false >/dev/null
else
  printf '{"secret":"%s"}' "$secret" | kcadm_stdin update "clients/$cid" -r sentinela -f -
fi
kcadm add-roles -r sentinela --uusername service-account-sentinela-admin-api --cclientid realm-management \
  --rolename view-users --rolename query-users --rolename manage-users
chmod 600 "$OUT"
echo "Development logins (user  password) — also in $OUT:"
cat "$OUT"

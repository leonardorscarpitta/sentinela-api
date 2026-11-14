# Keycloak with the Sentinela sign-in theme built in, for deployments that can't mount the theme folder (e.g. Kubernetes).
FROM quay.io/keycloak/keycloak:26.4
COPY --chown=1000:0 themes/sentinela /opt/keycloak/themes/sentinela

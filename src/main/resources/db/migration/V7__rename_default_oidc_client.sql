-- The OIDC client was renamed to follow the sentinela identifiers.
UPDATE organization_settings SET oidc_client_id = 'sentinela-web' WHERE oidc_client_id = 'sentinela-web';

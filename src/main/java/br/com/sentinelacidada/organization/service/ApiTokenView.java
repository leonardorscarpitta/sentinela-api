package br.com.sentinelacidada.organization.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import br.com.sentinelacidada.organization.domain.TokenScope;

/** {@code hint} shows only the token prefix and its last four characters. */
public record ApiTokenView(UUID id, String name, List<TokenScope> scopes, String hint, Instant createdAt,
		Instant expiresAt, Instant lastUsedAt, boolean expired) {
}

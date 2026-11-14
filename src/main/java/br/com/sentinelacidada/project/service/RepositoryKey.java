package br.com.sentinelacidada.project.service;

import java.time.Instant;
import java.util.UUID;

/** Public part of a repository's Sentinela-generated deploy key. */
public record RepositoryKey(UUID id, String publicKey, String fingerprint, Instant createdAt, Instant lastUsedAt) {
}

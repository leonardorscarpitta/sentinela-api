package br.com.sentinelacidada.organization.service;

import java.time.Instant;

import br.com.sentinelacidada.organization.domain.IntegrationCategory;
import br.com.sentinelacidada.organization.domain.IntegrationStatus;

/** {@code removable} is false for integrations the platform depends on (e.g. NIST NVD). */
public record IntegrationView(String id, String name, String shortCode, IntegrationCategory category,
		IntegrationStatus status, String detail, boolean removable, Instant lastSyncAt) {
}

package br.com.sentinelacidada.organization.service;

import java.util.UUID;

/** Published inside the creating transaction so other modules provision their defaults (policies). */
public record OrganizationCreated(UUID organizationId) {
}

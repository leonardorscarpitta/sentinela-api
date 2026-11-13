package br.com.sentinelacidada.organization.service;

import java.time.Instant;
import java.util.List;

import br.com.sentinelacidada.organization.domain.Feature;

public record AdminOrganizationView(String slug, String name, Instant createdAt, String ownerName, long memberCount,
		long repositoryCount, List<Feature> features) {
}

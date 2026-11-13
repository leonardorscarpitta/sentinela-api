package br.com.sentinelacidada.organization.service;

import java.util.Set;
import java.util.UUID;

import br.com.sentinelacidada.organization.domain.Feature;

/** Optional features the Sentinela administrators enabled for an organization; other modules use it to gate them. */
public interface OrganizationFeatures {

	boolean isEnabled(UUID organizationId, Feature feature);

	Set<Feature> enabledFor(UUID organizationId);
}

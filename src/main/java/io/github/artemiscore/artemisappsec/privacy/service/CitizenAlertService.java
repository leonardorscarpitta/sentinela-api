package io.github.artemiscore.artemisappsec.privacy.service;

import java.util.Optional;
import java.util.UUID;

/** Citizen alerts: privacy findings translated by AI into plain-language risk for citizens, cached per organization. */
public interface CitizenAlertService {

	/** The cached alert for this privacy finding, if any (no AI call); 400 {@code not-privacy-finding} for other kinds. */
	Optional<CitizenAlertView> find(UUID organizationId, UUID findingId);

	/**
	 * Returns the cached alert or writes one. Same gates as finding insights: the AI feature (403
	 * {@code feature-not-enabled}), a configured key (503 {@code ai-unavailable}), a free AI slot (503 {@code ai-busy})
	 * and the daily limit (429 {@code ai-limit}); an unusable answer is 502 {@code ai-failed} and nothing is cached.
	 */
	CitizenAlertView generate(UUID organizationId, UUID findingId);
}

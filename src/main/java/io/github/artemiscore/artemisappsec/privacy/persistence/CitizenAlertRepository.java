package io.github.artemiscore.artemisappsec.privacy.persistence;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import io.github.artemiscore.artemisappsec.privacy.domain.CitizenAlert;

public interface CitizenAlertRepository extends JpaRepository<CitizenAlert, UUID> {

	Optional<CitizenAlert> findByOrganizationIdAndCacheKeyAndLanguage(UUID organizationId, String cacheKey, String language);

	/** Any language, newest first: for when the organization changed its language after the alert was written. */
	Optional<CitizenAlert> findFirstByOrganizationIdAndCacheKeyOrderByCreatedAtDesc(UUID organizationId, String cacheKey);

	@Query("select distinct a.cacheKey from CitizenAlert a where a.organizationId = :organizationId")
	Set<String> cacheKeys(UUID organizationId);
}

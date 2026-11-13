package io.github.artemiscore.artemisappsec.privacy.persistence;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import io.github.artemiscore.artemisappsec.privacy.domain.CitizenRiskSnapshot;

public interface CitizenRiskSnapshotRepository extends JpaRepository<CitizenRiskSnapshot, UUID> {

	/** Newest first. */
	List<CitizenRiskSnapshot> findTop12ByOrganizationIdAndProjectIdInOrderByCreatedAtDescRunNumberDesc(UUID organizationId,
			Collection<UUID> projectIds);

	boolean existsByRunId(UUID runId);
}

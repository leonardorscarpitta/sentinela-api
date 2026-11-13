package br.com.sentinelacidada.policy.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.sentinelacidada.policy.domain.RemediationSla;

public interface RemediationSlaRepository extends JpaRepository<RemediationSla, UUID> {

	List<RemediationSla> findByOrganizationId(UUID organizationId);
}

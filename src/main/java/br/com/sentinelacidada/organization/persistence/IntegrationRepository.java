package br.com.sentinelacidada.organization.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.sentinelacidada.organization.domain.Integration;

public interface IntegrationRepository extends JpaRepository<Integration, UUID> {

	List<Integration> findByOrganizationIdOrderByPositionAsc(UUID organizationId);

	Optional<Integration> findByOrganizationIdAndCode(UUID organizationId, String code);
}

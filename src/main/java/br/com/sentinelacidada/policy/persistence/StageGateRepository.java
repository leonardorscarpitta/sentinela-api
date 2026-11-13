package br.com.sentinelacidada.policy.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.sentinelacidada.policy.domain.StageGate;

public interface StageGateRepository extends JpaRepository<StageGate, UUID> {

	List<StageGate> findByOrganizationId(UUID organizationId);
}

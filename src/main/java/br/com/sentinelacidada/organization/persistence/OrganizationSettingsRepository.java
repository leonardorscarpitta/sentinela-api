package br.com.sentinelacidada.organization.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.sentinelacidada.organization.domain.OrganizationSettings;

public interface OrganizationSettingsRepository extends JpaRepository<OrganizationSettings, UUID> {
}

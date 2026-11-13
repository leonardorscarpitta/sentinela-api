package br.com.sentinelacidada.privacy.api;

import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.sentinelacidada.organization.api.RequiresPermission;
import br.com.sentinelacidada.organization.domain.Permission;
import br.com.sentinelacidada.organization.service.Tenant;
import br.com.sentinelacidada.privacy.service.CitizenAlertService;
import br.com.sentinelacidada.privacy.service.CitizenAlertView;
import br.com.sentinelacidada.shared.error.ResourceNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/findings/{id}/citizen-alert")
@Tag(name = "Findings", description = "Vulnerabilities detected in monitored repositories")
class CitizenAlertController {

	private final CitizenAlertService alerts;

	CitizenAlertController(CitizenAlertService alerts) {
		this.alerts = alerts;
	}

	@GetMapping
	@Operation(summary = "Cached citizen alert of a privacy finding (404 when none was written yet)")
	CitizenAlertView get(Tenant tenant, @PathVariable UUID id) {
		return alerts.find(tenant.organizationId(), id)
			.orElseThrow(() -> new ResourceNotFoundException("Citizen alert for finding", id));
	}

	/** Triage permission: writing an alert spends the organization's AI budget. */
	@PostMapping
	@RequiresPermission(Permission.TRIAGE)
	@Operation(summary = "Translate a privacy finding into a citizen-risk alert with AI (cached per rule, category and output)")
	CitizenAlertView generate(Tenant tenant, @PathVariable UUID id) {
		return alerts.generate(tenant.organizationId(), id);
	}
}

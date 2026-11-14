package br.com.sentinelacidada.privacy.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.sentinelacidada.organization.service.Tenant;
import br.com.sentinelacidada.privacy.service.CitizenRisk;
import br.com.sentinelacidada.privacy.service.CitizenRiskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/v1/dashboard")
@Tag(name = "Dashboard", description = "Security overview across the organization's repositories")
class CitizenRiskController {

	private final CitizenRiskService risk;

	CitizenRiskController(CitizenRiskService risk) {
		this.risk = risk;
	}

	@GetMapping("/citizen-risk")
	@Operation(summary = "Risco Cidadão: personal-data exposure score, open leaks by category and trend",
			description = "repository: optional repository name (default: the whole organization)")
	CitizenRisk citizenRisk(Tenant tenant, @RequestParam(required = false) @Size(max = 255) String repository) {
		return risk.get(tenant.organizationId(), repository == null || repository.isBlank() ? null : repository);
	}
}

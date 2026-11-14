package br.com.sentinelacidada.organization.api;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.sentinelacidada.organization.domain.Permission;
import br.com.sentinelacidada.organization.service.IntegrationService;
import br.com.sentinelacidada.organization.service.IntegrationView;
import br.com.sentinelacidada.organization.service.Tenant;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/v1/integrations")
@Tag(name = "Integrations", description = "Source control, CI, notification, issue tracking and vulnerability data integrations")
class IntegrationController {

	record ConfigureIntegrationRequest(@Size(max = 255) String detail) {
	}

	private final IntegrationService integrations;

	IntegrationController(IntegrationService integrations) {
		this.integrations = integrations;
	}

	@GetMapping
	List<IntegrationView> list(Tenant tenant) {
		return integrations.findAll(tenant.organizationId());
	}

	@PostMapping("/{id}/connect")
	@RequiresPermission(Permission.INTEGRATIONS)
	IntegrationView connect(Tenant tenant, @PathVariable String id) {
		return integrations.connect(tenant.organizationId(), id);
	}

	@PostMapping("/{id}/disconnect")
	@RequiresPermission(Permission.INTEGRATIONS)
	IntegrationView disconnect(Tenant tenant, @PathVariable String id) {
		return integrations.disconnect(tenant.organizationId(), id);
	}

	@PutMapping("/{id}")
	@RequiresPermission(Permission.INTEGRATIONS)
	IntegrationView configure(Tenant tenant, @PathVariable String id,
			@Valid @RequestBody ConfigureIntegrationRequest request) {
		return integrations.configure(tenant.organizationId(), id, request.detail());
	}
}

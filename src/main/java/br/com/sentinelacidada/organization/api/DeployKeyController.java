package br.com.sentinelacidada.organization.api;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.sentinelacidada.organization.domain.Permission;
import br.com.sentinelacidada.organization.service.DeployKeyService;
import br.com.sentinelacidada.organization.service.DeployKeyView;
import br.com.sentinelacidada.organization.service.Tenant;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/deploy-keys")
@Tag(name = "Deploy keys", description = "Read-only SSH deploy keys; Sentinela generates one per private repository (see the repository access endpoints)")
class DeployKeyController {

	private final DeployKeyService keys;

	DeployKeyController(DeployKeyService keys) {
		this.keys = keys;
	}

	@GetMapping
	List<DeployKeyView> list(Tenant tenant) {
		return keys.findAll(tenant.organizationId());
	}

	@DeleteMapping("/{id}")
	@RequiresPermission(Permission.INTEGRATIONS)
	ResponseEntity<Void> revoke(Tenant tenant, @PathVariable UUID id) {
		keys.revoke(tenant.organizationId(), id);
		return ResponseEntity.noContent().build();
	}
}

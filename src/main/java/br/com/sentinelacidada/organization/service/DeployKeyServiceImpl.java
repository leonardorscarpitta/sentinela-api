package br.com.sentinelacidada.organization.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.sentinelacidada.organization.domain.DeployKey;
import br.com.sentinelacidada.organization.persistence.DeployKeyRepository;
import br.com.sentinelacidada.project.service.ProjectService;
import br.com.sentinelacidada.project.service.ProjectSummary;
import br.com.sentinelacidada.shared.error.ResourceNotFoundException;

@Service
@Transactional(readOnly = true)
class DeployKeyServiceImpl implements DeployKeyService {

	private final DeployKeyRepository keys;
	private final ProjectService projects;

	DeployKeyServiceImpl(DeployKeyRepository keys, ProjectService projects) {
		this.keys = keys;
		this.projects = projects;
	}

	@Override
	public List<DeployKeyView> findAll(UUID organizationId) {
		Map<UUID, String> names = projectNames(organizationId);
		return keys.findByProjectIdInOrderByCreatedAtAsc(names.keySet()).stream().map(k -> toView(k, names.get(k.getProjectId()))).toList();
	}

	@Override
	@Transactional
	public void revoke(UUID organizationId, UUID id) {
		DeployKey key = keys.findById(id)
			.filter(k -> projectNames(organizationId).containsKey(k.getProjectId()))
			.orElseThrow(() -> new ResourceNotFoundException("Deploy key", id));
		keys.delete(key);
		if (key.isManaged()) {
			projects.resetAccess(key.getProjectId());
		}
	}

	private Map<UUID, String> projectNames(UUID organizationId) {
		return projects.findAll(organizationId).stream().collect(Collectors.toMap(ProjectSummary::id, ProjectSummary::name));
	}

	private static DeployKeyView toView(DeployKey k, String repository) {
		return new DeployKeyView(k.getId(), k.getName(), k.getKeyType(), k.getFingerprint(), repository, k.getCreatedAt(),
				k.getLastUsedAt(), k.isManaged(), k.isManaged() ? k.getPublicKey() : null);
	}
}

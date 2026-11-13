package br.com.sentinelacidada.scanjob.service;

import java.util.List;
import java.util.UUID;

import br.com.sentinelacidada.project.service.SshCredentials;
import br.com.sentinelacidada.shared.domain.FailThreshold;
import br.com.sentinelacidada.shared.domain.Stage;

/**
 * What the scanner runner must do for one claimed run. {@code credentials} is set only for private (SSH)
 * repositories.
 */
public record JobAssignment(String type, UUID runId, long number, Repository repository, SshCredentials credentials,
		List<StageJob> stages, Limits limits) implements Assignment {

	/**
	 * @param paths monorepo folders to scan inside the clone; empty for the whole repository
	 * @param image published container image for the image stage, or null
	 * @param access {@code public} (HTTPS) or {@code ssh}
	 */
	public record Repository(String name, String cloneUrl, String branch, List<String> paths, String image, String access) {
	}

	public record StageJob(Stage id, String tool, FailThreshold failOn) {
	}

	public record Limits(int stageTimeoutSeconds, int maxRepoSizeMb) {
	}
}

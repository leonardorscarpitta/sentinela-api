package br.com.sentinelacidada.pipelinerun.service;

import java.time.Instant;
import java.util.List;

import br.com.sentinelacidada.pipelinerun.domain.RunStatus;
import br.com.sentinelacidada.pipelinerun.domain.RunTrigger;

/** {@code repositoryRemoved}: the run belongs to a removed repository whose history was kept. */
public record RunView(long number, String repository, boolean repositoryRemoved, String branch, String commit, String message, RunTrigger trigger,
		String actor, RunStatus status, Instant startedAt, int durationSeconds, int newFindings, int totalFindings,
		boolean failedByPolicy, List<String> folders,
		boolean quotaRefunded) {
}

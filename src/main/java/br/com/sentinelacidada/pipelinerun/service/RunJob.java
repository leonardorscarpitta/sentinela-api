package br.com.sentinelacidada.pipelinerun.service;

import java.util.List;
import java.util.UUID;

import br.com.sentinelacidada.pipelinerun.domain.RunStatus;

/** Internal view of a run for the scanner job API: which project it scans and which stages are still pending. */
public record RunJob(UUID runId, long number, UUID projectId, RunStatus status, List<StageAssignment> pendingStages) {
}

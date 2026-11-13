package io.github.artemiscore.artemisappsec.pipelinerun.service;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import io.github.artemiscore.artemisappsec.shared.domain.Stage;

/**
 * Published when the scanner completes a run. {@code scannedStages}: stages whose tool ran to the end (passed, warning
 * or failed by the policy gate), so their findings are up to date.
 */
public record RunCompleted(UUID runId, long number, UUID projectId, Instant completedAt, Set<Stage> scannedStages) {
}

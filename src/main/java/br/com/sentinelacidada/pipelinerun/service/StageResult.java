package br.com.sentinelacidada.pipelinerun.service;

import br.com.sentinelacidada.pipelinerun.domain.StageOutcome;
import br.com.sentinelacidada.shared.domain.Severity;
import br.com.sentinelacidada.shared.domain.Stage;

/**
 * A stage result reported by the scanner runner, after its findings were recorded.
 * {@code highestSeverity} is null when there were no findings.
 */
public record StageResult(Stage stage, String tool, StageOutcome outcome, int durationSeconds, String log, int findings,
		Severity highestSeverity, int newFindings) {
}

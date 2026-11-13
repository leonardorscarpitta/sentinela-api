package br.com.sentinelacidada.pipelinerun.service;

import java.util.List;

import br.com.sentinelacidada.pipelinerun.domain.StageStatus;
import br.com.sentinelacidada.shared.domain.Phase;
import br.com.sentinelacidada.shared.domain.Severity;
import br.com.sentinelacidada.shared.domain.Stage;

/** {@code failedBy} is the severity threshold that failed the stage, or null. */
public record StageRunView(Stage id, Phase phase, String tool, StageStatus status, int durationSeconds, int findings,
		Severity failedBy, List<String> log) {
}

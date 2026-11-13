package br.com.sentinelacidada.pipelinerun.service;

import br.com.sentinelacidada.shared.domain.FailThreshold;
import br.com.sentinelacidada.shared.domain.Stage;

/** {@code failOn} is null for stages that never fail the pipeline. */
public record StageAssignment(Stage id, String tool, FailThreshold failOn) {
}

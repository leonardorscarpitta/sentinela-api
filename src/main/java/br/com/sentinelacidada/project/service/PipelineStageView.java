package br.com.sentinelacidada.project.service;

import java.util.List;

import br.com.sentinelacidada.shared.domain.FailThreshold;
import br.com.sentinelacidada.shared.domain.Phase;
import br.com.sentinelacidada.shared.domain.Stage;

/** {@code failOn} is null for artifact-only stages. */
public record PipelineStageView(Stage id, Phase phase, List<String> tools, String tool, boolean enabled,
		FailThreshold failOn, int minutes, boolean available) {

	public static PipelineStageView defaults(Stage stage) {
		return new PipelineStageView(stage, stage.phase(), stage.tools(), stage.defaultTool(), stage.enabledByDefault(),
				stage.defaultFailOn(), stage.estimatedMinutes(), stage.available());
	}
}

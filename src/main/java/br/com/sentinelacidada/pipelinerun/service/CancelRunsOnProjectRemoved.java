package br.com.sentinelacidada.pipelinerun.service;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import br.com.sentinelacidada.project.service.ProjectRemoved;

/** A removed repository is never scanned again: its queued and running runs are cancelled with the removal. */
@Component
class CancelRunsOnProjectRemoved {

	private final PipelineRunService runs;

	CancelRunsOnProjectRemoved(PipelineRunService runs) {
		this.runs = runs;
	}

	@EventListener
	void onProjectRemoved(ProjectRemoved event) {
		runs.cancelActive(event.projectId());
	}
}

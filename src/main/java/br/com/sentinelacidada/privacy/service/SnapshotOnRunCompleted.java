package br.com.sentinelacidada.privacy.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import br.com.sentinelacidada.pipelinerun.service.RunCompleted;
import br.com.sentinelacidada.shared.domain.Stage;

/** Records the Risco Cidadão trend point once a run that scanned the pii stage is committed as complete. */
@Component
class SnapshotOnRunCompleted {

	private static final Logger log = LoggerFactory.getLogger(SnapshotOnRunCompleted.class);

	private final CitizenRiskService risk;

	SnapshotOnRunCompleted(CitizenRiskService risk) {
		this.risk = risk;
	}

	/** After the completion is committed; {@link CitizenRiskService#recordSnapshot} runs in its own transaction. */
	@TransactionalEventListener
	void onRunCompleted(RunCompleted event) {
		if (!event.scannedStages().contains(Stage.PII)) {
			return;
		}
		try {
			risk.recordSnapshot(event);
		}
		catch (RuntimeException e) {
			// The run is already complete; a missing trend point must not surface as a scanner error.
			log.warn("Citizen risk snapshot of run #{} was not recorded ({})", event.number(), e.getClass().getSimpleName());
		}
	}
}

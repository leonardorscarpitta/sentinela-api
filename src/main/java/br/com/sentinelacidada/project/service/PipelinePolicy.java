package br.com.sentinelacidada.project.service;

import java.util.Map;
import java.util.UUID;

import br.com.sentinelacidada.shared.domain.FailThreshold;
import br.com.sentinelacidada.shared.domain.Stage;

/** Organization-wide pipeline rules (stage gates), implemented by the policy module. */
public interface PipelinePolicy {

	/** The organization's gate per stage; stages without a gate are absent. */
	Map<Stage, Gate> gates(UUID organizationId);

	/**
	 * @param required repositories cannot disable the stage
	 * @param minimumFailOn loosest threshold a repository may use; null for stages that never fail
	 */
	record Gate(boolean required, FailThreshold minimumFailOn) {

		/** Whether {@code failOn} is at least as strict as the organization's minimum. */
		public boolean allows(FailThreshold failOn) {
			return minimumFailOn == null || (failOn != null && failOn.ordinal() >= minimumFailOn.ordinal());
		}

		/** The stricter of the repository's threshold and the organization's minimum. */
		public FailThreshold strictest(FailThreshold failOn) {
			return allows(failOn) ? failOn : minimumFailOn;
		}
	}
}

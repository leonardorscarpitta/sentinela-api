package br.com.sentinelacidada.privacy.service;

import java.util.UUID;

import br.com.sentinelacidada.pipelinerun.service.RunCompleted;

/** Risco Cidadão: the executive view of personal-data exposure (see {@link CitizenRiskScore} for the formula). */
public interface CitizenRiskService {

	/** The whole organization (connected repositories) or, when {@code repository} is set, that repository (404 if unknown). */
	CitizenRisk get(UUID organizationId, String repository);

	/** Stores the repository's score at the end of a run that ran the pii stage (once per run). */
	void recordSnapshot(RunCompleted run);
}

package br.com.sentinelacidada.privacy.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Risco Cidadão of one repository at the end of a completed run that ran the pii stage. */
@Entity
@Table(name = "citizen_risk_snapshot")
public class CitizenRiskSnapshot {

	@Id
	private UUID id;

	@Column(name = "organization_id", nullable = false)
	private UUID organizationId;

	@Column(name = "project_id", nullable = false)
	private UUID projectId;

	@Column(name = "run_id")
	private UUID runId;

	@Column(name = "run_number", nullable = false)
	private long runNumber;

	@Column(nullable = false)
	private int score;

	@Column(name = "open_flows", nullable = false)
	private int openFlows;

	@Column(name = "mapped_data", nullable = false)
	private int mappedData;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected CitizenRiskSnapshot() {
	}

	public CitizenRiskSnapshot(UUID organizationId, UUID projectId, UUID runId, long runNumber, int score, int openFlows,
			int mappedData, Instant createdAt) {
		this.id = UUID.randomUUID();
		this.organizationId = organizationId;
		this.projectId = projectId;
		this.runId = runId;
		this.runNumber = runNumber;
		this.score = score;
		this.openFlows = openFlows;
		this.mappedData = mappedData;
		this.createdAt = createdAt;
	}

	public UUID getProjectId() {
		return projectId;
	}

	public long getRunNumber() {
		return runNumber;
	}

	public int getScore() {
		return score;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}

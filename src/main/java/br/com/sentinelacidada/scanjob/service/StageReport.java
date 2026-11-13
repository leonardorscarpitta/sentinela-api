package br.com.sentinelacidada.scanjob.service;

import java.util.List;

import br.com.sentinelacidada.finding.service.DetectedIssue;
import br.com.sentinelacidada.pipelinerun.domain.StageOutcome;

public record StageReport(StageOutcome status, String tool, int durationSeconds, String log, List<DetectedIssue> findings) {
}

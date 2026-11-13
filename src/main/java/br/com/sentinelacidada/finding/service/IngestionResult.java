package br.com.sentinelacidada.finding.service;

import br.com.sentinelacidada.shared.domain.Severity;

/**
 * Outcome of recording one stage's detections. {@code highestSeverity} is null when nothing was reported.
 */
public record IngestionResult(int reported, int newFindings, int reopened, int fixed, Severity highestSeverity) {
}

package br.com.sentinelacidada.finding.service;

import br.com.sentinelacidada.finding.domain.FindingKind;
import br.com.sentinelacidada.finding.domain.FindingStatus;
import br.com.sentinelacidada.shared.domain.Severity;

public record FindingQuery(String text, FindingKind kind, Severity severity, FindingStatus status, String repository,
		SortField sort, boolean ascending) {

	public enum SortField {
		CVSS, DETECTED
	}
}

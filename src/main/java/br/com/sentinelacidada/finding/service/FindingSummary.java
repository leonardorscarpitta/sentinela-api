package br.com.sentinelacidada.finding.service;

import java.time.Instant;
import java.util.UUID;

import br.com.sentinelacidada.finding.domain.Ecosystem;
import br.com.sentinelacidada.finding.domain.FindingKind;
import br.com.sentinelacidada.finding.domain.FindingStatus;
import br.com.sentinelacidada.shared.domain.Severity;
import br.com.sentinelacidada.shared.domain.Stage;

/**
 * {@code cve}, {@code cvss}, package fields and {@code ecosystem} are null for kinds that are not package based;
 * {@code location} is null for package-based kinds. {@code repositoryRemoved}: the repository was removed and the
 * finding is kept as history. {@code cwe}: weakness id such as {@code CWE-79}, when the tool reported one.
 * {@code privacy}: personal-data details of {@code privacy} findings, null for the other kinds.
 */
public record FindingSummary(UUID id, String reference, FindingKind kind, String ruleId, String cve, String repository,
		boolean repositoryRemoved,
		String packageName, String packageGroup, String installedVersion, String fixedVersion, Ecosystem ecosystem,
		String title, Double cvss, Severity severity, FindingStatus status, Stage stage, String tool,
		Location location, Instant detectedAt, Instant statusChangedAt, Instant dueAt, boolean overdue, String cwe,
		Privacy privacy) {

	public record Location(String path, Integer line) {
	}
}

package br.com.sentinelacidada.report.service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import br.com.sentinelacidada.report.domain.ReportAuthor;
import br.com.sentinelacidada.report.domain.ReportContent.Narrative;
import br.com.sentinelacidada.report.domain.ReportSection;
import br.com.sentinelacidada.report.domain.ReportStatus;
import br.com.sentinelacidada.report.domain.ReportType;

/**
 * {@code scope} is the repository name, or {@code null} for the whole organization. {@code error} is a short code
 * ({@code generation-failed}, {@code too-many-attempts}) when the report failed; {@code narrative} says who wrote its
 * texts once it is ready.
 */
public record ReportSummary(UUID id, String title, ReportType type, Set<ReportSection> sections, String scope,
		LocalDate periodStart, LocalDate periodEnd, Instant generatedAt, ReportAuthor generatedBy, ReportStatus status,
		String error, Narrative narrative) {
}

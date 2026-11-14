package br.com.sentinelacidada.finding.service;

import java.util.List;

import br.com.sentinelacidada.finding.domain.PrivacyCategory;
import br.com.sentinelacidada.finding.domain.PrivacySink;
import br.com.sentinelacidada.finding.domain.PrivacyType;
import br.com.sentinelacidada.finding.domain.TraceStep;

/**
 * Personal-data details of a {@code privacy} finding. {@code sink} is null and {@code trace} empty for mappings.
 * The trace holds locations only (relative path and line): never code or values.
 */
public record Privacy(PrivacyCategory category, PrivacyType type, PrivacySink sink, List<TraceStep> trace) {
}

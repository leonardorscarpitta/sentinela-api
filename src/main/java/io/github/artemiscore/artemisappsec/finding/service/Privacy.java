package io.github.artemiscore.artemisappsec.finding.service;

import java.util.List;

import io.github.artemiscore.artemisappsec.finding.domain.PrivacyCategory;
import io.github.artemiscore.artemisappsec.finding.domain.PrivacySink;
import io.github.artemiscore.artemisappsec.finding.domain.PrivacyType;
import io.github.artemiscore.artemisappsec.finding.domain.TraceStep;

/**
 * Personal-data details of a {@code privacy} finding. {@code sink} is null and {@code trace} empty for mappings.
 * The trace holds locations only (relative path and line): never code or values.
 */
public record Privacy(PrivacyCategory category, PrivacyType type, PrivacySink sink, List<TraceStep> trace) {
}

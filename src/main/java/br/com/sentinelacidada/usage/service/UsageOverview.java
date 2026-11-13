package br.com.sentinelacidada.usage.service;

import java.time.Instant;
import java.util.List;

import br.com.sentinelacidada.organization.domain.Feature;

/**
 * Scans used in the current calendar month (UTC) and the optional features enabled for the organization.
 *
 * @param resetsAt start of the next month (UTC), when the counter starts again
 */
public record UsageOverview(long scansThisMonth, int scanLimit, Instant periodStart, Instant resetsAt, List<Feature> features) {
}

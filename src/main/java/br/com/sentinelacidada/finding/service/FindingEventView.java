package br.com.sentinelacidada.finding.service;

import java.time.Instant;

import br.com.sentinelacidada.finding.domain.FindingEventType;
import br.com.sentinelacidada.finding.domain.FindingStatus;

public record FindingEventView(FindingEventType type, FindingStatus status, String note, Instant occurredAt) {
}

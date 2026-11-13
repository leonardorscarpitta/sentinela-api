package br.com.sentinelacidada.project.service;

import java.time.Instant;
import java.util.UUID;

import br.com.sentinelacidada.project.domain.AccessCheckStatus;

public record AccessCheckView(UUID id, AccessCheckStatus status, String message, Instant requestedAt, Instant finishedAt) {
}

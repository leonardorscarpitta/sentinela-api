package br.com.sentinelacidada.finding.domain;

/**
 * One step of a privacy flow: {@code role} is {@code source}, {@code propagation} or {@code sink}; {@code path} is
 * relative to the repository root.
 */
public record TraceStep(String role, String path, Integer line) {
}

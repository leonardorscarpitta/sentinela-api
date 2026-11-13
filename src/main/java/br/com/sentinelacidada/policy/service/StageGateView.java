package br.com.sentinelacidada.policy.service;

import br.com.sentinelacidada.shared.domain.FailThreshold;
import br.com.sentinelacidada.shared.domain.Stage;
import jakarta.validation.constraints.NotNull;

/** {@code failOn} is {@code null} for artifact-only stages. */
public record StageGateView(@NotNull Stage stage, FailThreshold failOn, boolean required) {
}

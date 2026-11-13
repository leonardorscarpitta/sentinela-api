package br.com.sentinelacidada.policy.service;

import br.com.sentinelacidada.shared.domain.Severity;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record SlaView(@NotNull Severity severity, @Min(1) @Max(365) int days) {
}

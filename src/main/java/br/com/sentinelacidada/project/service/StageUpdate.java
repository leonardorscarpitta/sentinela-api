package br.com.sentinelacidada.project.service;

import br.com.sentinelacidada.shared.domain.FailThreshold;
import br.com.sentinelacidada.shared.domain.Stage;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record StageUpdate(@NotNull Stage id, boolean enabled, @NotBlank String tool, FailThreshold failOn) {
}

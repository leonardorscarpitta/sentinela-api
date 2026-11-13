package br.com.sentinelacidada.project.service;

import java.util.List;

import br.com.sentinelacidada.project.domain.Stack;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateProjectCommand(
		@NotBlank @Size(max = 512) @Pattern(regexp = "^(git@[\\w.-]+:[\\w./-]+\\.git|ssh://[\\w.@:/-]+)$", message = "must be an SSH repository URL such as git@git.example.com:owner/repo.git (Sentinela only scans repositories through a deploy key)") String url,
		@NotBlank @Pattern(regexp = "^[\\w./-]{1,255}$") String branch,
		Stack stack,
		@NotNull @Valid ProjectSettings settings,
		@NotNull List<@Valid StageUpdate> pipeline) {
}

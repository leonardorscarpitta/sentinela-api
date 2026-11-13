package br.com.sentinelacidada.pipelinerun.service;

import br.com.sentinelacidada.pipelinerun.domain.RunStatus;

public record RunFilter(String repository, RunStatus status, String branch) {
}

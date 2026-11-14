package br.com.sentinelacidada.project.service;

import java.util.List;

public record ProjectDetails(ProjectSummary project, ProjectSettings settings, List<PipelineStageView> pipeline) {
}

package br.com.sentinelacidada.privacy.service;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import br.com.sentinelacidada.finding.domain.FindingKind;
import br.com.sentinelacidada.finding.domain.PrivacyCategory;
import br.com.sentinelacidada.finding.service.FindingService;
import br.com.sentinelacidada.finding.service.FindingSummary;
import br.com.sentinelacidada.pipelinerun.service.RunCompleted;
import br.com.sentinelacidada.privacy.domain.CitizenRiskSnapshot;
import br.com.sentinelacidada.privacy.persistence.CitizenAlertRepository;
import br.com.sentinelacidada.privacy.persistence.CitizenRiskSnapshotRepository;
import br.com.sentinelacidada.project.service.ProjectService;
import br.com.sentinelacidada.project.service.ProjectSummary;
import br.com.sentinelacidada.shared.error.ResourceNotFoundException;

@Service
@Transactional(readOnly = true)
class CitizenRiskServiceImpl implements CitizenRiskService {

	private final FindingService findings;
	private final ProjectService projects;
	private final CitizenRiskSnapshotRepository snapshots;
	private final CitizenAlertRepository alerts;

	CitizenRiskServiceImpl(FindingService findings, ProjectService projects, CitizenRiskSnapshotRepository snapshots,
			CitizenAlertRepository alerts) {
		this.findings = findings;
		this.projects = projects;
		this.snapshots = snapshots;
		this.alerts = alerts;
	}

	@Override
	public CitizenRisk get(UUID organizationId, String repository) {
		List<ProjectSummary> connected = projects.findAll(organizationId);
		if (repository != null) {
			connected = connected.stream().filter(p -> p.name().equals(repository)).toList();
			if (connected.isEmpty()) {
				throw new ResourceNotFoundException("Repository", repository);
			}
		}
		List<FindingSummary> open = openPrivacy(organizationId, repository);
		double sum = CitizenRiskScore.sum(open);
		int score = CitizenRiskScore.score(sum);
		long flows = open.stream().filter(CitizenRiskScore::isFlow).count();
		Set<String> written = alerts.cacheKeys(organizationId);
		long withAlert = open.stream().filter(f -> written.contains(CitizenAlertPayload.cacheKey(f))).count();
		long repositories = open.stream().map(FindingSummary::repository).distinct().count();

		List<CitizenRisk.CategoryRisk> byCategory = Arrays.stream(PrivacyCategory.values())
			.map(c -> {
				List<FindingSummary> ofCategory = open.stream().filter(f -> f.privacy().category() == c).toList();
				long categoryFlows = ofCategory.stream().filter(CitizenRiskScore::isFlow).count();
				return new CitizenRisk.CategoryRisk(c, round1(CitizenRiskScore.sum(ofCategory)), categoryFlows,
						ofCategory.size() - categoryFlows);
			})
			.sorted(Comparator.comparingDouble(CitizenRisk.CategoryRisk::weight).reversed())
			.toList();

		Map<UUID, String> names = connected.stream().collect(Collectors.toMap(ProjectSummary::id, ProjectSummary::name));
		List<CitizenRisk.TrendPoint> trend = names.isEmpty() ? List.of()
				: snapshots.findTop12ByOrganizationIdAndProjectIdInOrderByCreatedAtDescRunNumberDesc(organizationId, names.keySet())
					.reversed().stream()
					.map(s -> new CitizenRisk.TrendPoint(s.getRunNumber(), names.get(s.getProjectId()), s.getCreatedAt(),
							s.getScore()))
					.toList();

		return new CitizenRisk(score, CitizenRiskScore.level(score), flows > 0 ? "blocked" : "approved", flows,
				open.size() - flows, withAlert, repositories, byCategory, trend, CitizenRiskScore.WEIGHTS);
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void recordSnapshot(RunCompleted run) {
		ProjectSummary project = projects.findById(run.projectId()).orElse(null);
		if (project == null || project.removed() || snapshots.existsByRunId(run.runId())) {
			return;
		}
		List<FindingSummary> open = openPrivacy(project.organizationId(), project.name());
		long flows = open.stream().filter(CitizenRiskScore::isFlow).count();
		snapshots.save(new CitizenRiskSnapshot(project.organizationId(), project.id(), run.runId(), run.number(),
				CitizenRiskScore.score(CitizenRiskScore.sum(open)), (int) flows, (int) (open.size() - flows),
				run.completedAt()));
	}

	/** Open or triaged privacy findings of connected repositories (all of them, or the named one). */
	private List<FindingSummary> openPrivacy(UUID organizationId, String repository) {
		return findings.findActive(organizationId).stream()
			.filter(f -> f.kind() == FindingKind.PRIVACY && f.privacy() != null)
			.filter(f -> repository == null || repository.equals(f.repository()))
			.toList();
	}

	private static double round1(double value) {
		return Math.round(value * 10) / 10.0;
	}
}

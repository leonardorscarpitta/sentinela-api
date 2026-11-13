package br.com.sentinelacidada.policy.service;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import br.com.sentinelacidada.finding.service.FindingPolicy;
import br.com.sentinelacidada.policy.domain.LicenseDecision;
import br.com.sentinelacidada.policy.domain.RemediationSla;
import br.com.sentinelacidada.policy.domain.StageGate;
import br.com.sentinelacidada.policy.persistence.LicenseRuleRepository;
import br.com.sentinelacidada.policy.persistence.RemediationSlaRepository;
import br.com.sentinelacidada.policy.persistence.StageGateRepository;
import br.com.sentinelacidada.project.service.PipelinePolicy;
import br.com.sentinelacidada.shared.domain.Severity;
import br.com.sentinelacidada.shared.domain.Stage;

/** Makes the stored policies effective for pipelines and findings (only repositories, so no module cycles). */
@Component
@Transactional(readOnly = true)
class PolicyRules implements PipelinePolicy, FindingPolicy {

	private final StageGateRepository gates;
	private final RemediationSlaRepository slas;
	private final LicenseRuleRepository licenses;

	PolicyRules(StageGateRepository gates, RemediationSlaRepository slas, LicenseRuleRepository licenses) {
		this.gates = gates;
		this.slas = slas;
		this.licenses = licenses;
	}

	@Override
	public Map<Stage, Gate> gates(UUID organizationId) {
		return gates.findByOrganizationId(organizationId).stream()
			.collect(Collectors.toMap(StageGate::getStage, g -> new Gate(g.isRequired(), g.getFailOn())));
	}

	@Override
	public Map<Severity, Integer> remediationDays(UUID organizationId) {
		return slas.findByOrganizationId(organizationId).stream()
			.collect(Collectors.toMap(RemediationSla::getSeverity, RemediationSla::getDays));
	}

	@Override
	public LicenseVerdict license(UUID organizationId, String licenseId) {
		String id = licenseId.toLowerCase(Locale.ROOT);
		return licenses.findByOrganizationIdOrderByLicenseIdAsc(organizationId).stream()
			.filter(rule -> rule.getLicenseId().toLowerCase(Locale.ROOT).equals(id))
			.findFirst()
			.map(rule -> rule.getDecision() == LicenseDecision.ALLOWED ? LicenseVerdict.ALLOW : LicenseVerdict.DENY)
			.orElse(LicenseVerdict.REVIEW);
	}
}

package io.github.artemiscore.artemisappsec.privacy.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import io.github.artemiscore.artemisappsec.finding.domain.FindingKind;
import io.github.artemiscore.artemisappsec.finding.service.FindingDetails;
import io.github.artemiscore.artemisappsec.finding.service.FindingService;
import io.github.artemiscore.artemisappsec.finding.service.FindingSummary;
import io.github.artemiscore.artemisappsec.organization.domain.Feature;
import io.github.artemiscore.artemisappsec.organization.service.OrganizationFeatures;
import io.github.artemiscore.artemisappsec.organization.service.OrganizationSettingsService;
import io.github.artemiscore.artemisappsec.privacy.domain.AlertContent;
import io.github.artemiscore.artemisappsec.privacy.domain.CitizenAlert;
import io.github.artemiscore.artemisappsec.privacy.persistence.CitizenAlertRepository;
import io.github.artemiscore.artemisappsec.project.service.ProjectFolder;
import io.github.artemiscore.artemisappsec.project.service.ProjectService;
import io.github.artemiscore.artemisappsec.project.service.ProjectSummary;
import io.github.artemiscore.artemisappsec.shared.ai.AiUsageCounter;
import io.github.artemiscore.artemisappsec.shared.ai.ClaudeClient;
import io.github.artemiscore.artemisappsec.shared.config.ArtemisAppSecProperties;
import io.github.artemiscore.artemisappsec.shared.error.ApiProblemException;

@Service
class CitizenAlertServiceImpl implements CitizenAlertService {

	private final FindingService findings;
	private final ProjectService projects;
	private final OrganizationFeatures features;
	private final OrganizationSettingsService settings;
	private final ClaudeClient claude;
	private final CitizenAlertRepository alerts;
	private final AiUsageCounter usage;
	private final int alertsPerDay;
	private final Clock clock = Clock.systemUTC();

	CitizenAlertServiceImpl(FindingService findings, ProjectService projects, OrganizationFeatures features,
			OrganizationSettingsService settings, ClaudeClient claude, CitizenAlertRepository alerts, AiUsageCounter usage,
			ArtemisAppSecProperties properties) {
		this.findings = findings;
		this.projects = projects;
		this.features = features;
		this.settings = settings;
		this.claude = claude;
		this.alerts = alerts;
		this.usage = usage;
		this.alertsPerDay = properties.ai().insightsPerDay();
	}

	@Override
	public Optional<CitizenAlertView> find(UUID organizationId, UUID findingId) {
		String key = CitizenAlertPayload.cacheKey(privacyFinding(organizationId, findingId).finding());
		return alerts.findByOrganizationIdAndCacheKeyAndLanguage(organizationId, key, language(organizationId))
			.or(() -> alerts.findFirstByOrganizationIdAndCacheKeyOrderByCreatedAtDesc(organizationId, key))
			.map(CitizenAlertServiceImpl::toView);
	}

	@Override
	public CitizenAlertView generate(UUID organizationId, UUID findingId) {
		if (!features.isEnabled(organizationId, Feature.AI_TRIAGE)) {
			throw new ApiProblemException(HttpStatus.FORBIDDEN, "feature-not-enabled",
					"AI features are not enabled for this organization; ask the Sentinela Cidadã administrators",
					Map.of("feature", Feature.AI_TRIAGE.code()));
		}
		FindingDetails details = privacyFinding(organizationId, findingId);
		String key = CitizenAlertPayload.cacheKey(details.finding());
		// Only the organization's language is written, to avoid paying for a translation nobody reads.
		String language = language(organizationId);
		Optional<CitizenAlert> cached = alerts.findByOrganizationIdAndCacheKeyAndLanguage(organizationId, key, language);
		if (cached.isPresent()) {
			return toView(cached.get());
		}
		if (!claude.configured()) {
			throw new ApiProblemException(HttpStatus.SERVICE_UNAVAILABLE, "ai-unavailable",
					"AI is not configured on this server; ask the Sentinela Cidadã administrators");
		}
		if (!claude.tryAcquire(Duration.ZERO)) {
			throw new ApiProblemException(HttpStatus.SERVICE_UNAVAILABLE, "ai-busy", "The AI is busy; try again in a minute");
		}
		AlertContent content;
		try {
			if (!usage.tryReserve(organizationId, LocalDate.now(clock), AiUsageCounter.CITIZEN_ALERT, alertsPerDay)) {
				throw new ApiProblemException(HttpStatus.TOO_MANY_REQUESTS, "ai-limit",
						"Your organization reached today’s limit of %d citizen alerts; try again tomorrow (UTC)"
							.formatted(alertsPerDay),
						Map.of("limit", alertsPerDay));
			}
			FindingSummary f = details.finding();
			content = claude
				.json(CitizenAlertPayload.SYSTEM_INSTRUCTION,
						CitizenAlertPayload.of(details, stack(organizationId, f.repository()), language),
						CitizenAlertPayload.RESPONSE_SCHEMA, CitizenAlertPayload.MAX_TOKENS, CitizenAlertPayload.Answer.class)
				.flatMap(a -> CitizenAlertPayload.validated(a, f.severity().wireValue()))
				.orElseThrow(() -> new ApiProblemException(HttpStatus.BAD_GATEWAY, "ai-failed",
						"The citizen alert could not be written; try again later"));
		}
		finally {
			claude.release();
		}
		try {
			return toView(alerts.saveAndFlush(
					new CitizenAlert(organizationId, key, language, content, claude.model(), Instant.now(clock))));
		}
		catch (DataIntegrityViolationException e) {
			// Someone else wrote it at the same time: keep theirs.
			return alerts.findByOrganizationIdAndCacheKeyAndLanguage(organizationId, key, language)
				.map(CitizenAlertServiceImpl::toView)
				.orElseThrow(() -> e);
		}
	}

	/** 404 for unknown findings (or of another organization), 400 for findings that are not about personal data. */
	private FindingDetails privacyFinding(UUID organizationId, UUID findingId) {
		FindingDetails details = findings.get(organizationId, findingId);
		if (details.finding().kind() != FindingKind.PRIVACY || details.finding().privacy() == null) {
			throw new ApiProblemException(HttpStatus.BAD_REQUEST, "not-privacy-finding",
					"Citizen alerts are only available for privacy findings");
		}
		return details;
	}

	private String language(UUID organizationId) {
		return settings.get(organizationId).general().defaultLanguage();
	}

	/** "Java + Spring", or the folder stacks of a monorepo; null while not detected. */
	private String stack(UUID organizationId, String repository) {
		return projects.findAllIncludingRemoved(organizationId).stream()
			.filter(p -> p.name().equals(repository))
			.findFirst()
			.map(CitizenAlertServiceImpl::stackLabel)
			.orElse(null);
	}

	private static String stackLabel(ProjectSummary p) {
		if (p.folders() != null && p.folders().size() > 1) {
			return p.folders().stream().map(ProjectFolder::stack).distinct().map(s -> s.label())
				.collect(Collectors.joining(", "));
		}
		return p.stack() == null ? null : p.stack().label();
	}

	private static CitizenAlertView toView(CitizenAlert a) {
		AlertContent c = a.getContent();
		return new CitizenAlertView(c.title(), c.whatHappens(), c.whoIsAffected(), c.citizenGuidance(), c.technicalFix(),
				c.severity(), a.getLanguage(), a.getModel(), a.getCreatedAt());
	}
}

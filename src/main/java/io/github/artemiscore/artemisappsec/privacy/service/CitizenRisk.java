package io.github.artemiscore.artemisappsec.privacy.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import io.github.artemiscore.artemisappsec.finding.domain.PrivacyCategory;

/**
 * Risco Cidadão of the organization (or one repository): how exposed the citizens' personal data is in the code.
 * {@code level}: {@code critical} (≥ 75), {@code high} (≥ 50), {@code moderate} (≥ 25) or {@code low};
 * {@code verdict}: {@code blocked} while any privacy flow is open, else {@code approved}. {@code citizenAlerts}: open
 * privacy findings with a citizen alert written; {@code repositories}: repositories with open privacy findings.
 */
public record CitizenRisk(int score, String level, String verdict, long openFlows, long mappedData, long citizenAlerts,
		long repositories, List<CategoryRisk> byCategory, List<TrendPoint> trend, Weights weights) {

	/** {@code weight}: this category's share of the score's sum, before saturation. */
	public record CategoryRisk(PrivacyCategory category, double weight, long flows, long mapped) {
	}

	public record TrendPoint(long runNumber, String repository, Instant date, int score) {
	}

	public record Weights(Map<String, Double> severity, Map<String, Double> category, Map<String, Double> sink,
			double latentFactor, double saturation) {
	}
}

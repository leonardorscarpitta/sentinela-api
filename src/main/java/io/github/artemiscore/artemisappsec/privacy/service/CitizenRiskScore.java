package io.github.artemiscore.artemisappsec.privacy.service;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import io.github.artemiscore.artemisappsec.finding.domain.PrivacyCategory;
import io.github.artemiscore.artemisappsec.finding.domain.PrivacySink;
import io.github.artemiscore.artemisappsec.finding.domain.PrivacyType;
import io.github.artemiscore.artemisappsec.finding.service.FindingSummary;
import io.github.artemiscore.artemisappsec.shared.domain.Severity;

/**
 * The Risco Cidadão formula. Each open privacy finding weighs severity × category × sink (flows) or
 * severity × category × latent factor (mapped data, not leaking yet); the sum Σ is saturated into 0–100 with
 * {@code score = round(100 × (1 − e^(−Σ / saturation)))}, so a few critical leaks already score high and the score
 * never passes 100.
 */
final class CitizenRiskScore {

	static final Map<Severity, Double> SEVERITY = new EnumMap<>(Map.of(Severity.CRITICAL, 10.0, Severity.HIGH, 6.0,
			Severity.MEDIUM, 3.0, Severity.LOW, 1.0));
	static final Map<PrivacyCategory, Double> CATEGORY = new EnumMap<>(Map.of(PrivacyCategory.HEALTH, 1.5,
			PrivacyCategory.CREDENTIAL, 1.4, PrivacyCategory.FINANCIAL, 1.3, PrivacyCategory.IDENTIFICATION, 1.2,
			PrivacyCategory.CONTACT, 0.8));
	static final Map<PrivacySink, Double> SINK = new EnumMap<>(Map.of(PrivacySink.EXTERNAL_API, 1.6,
			PrivacySink.HTTP_RESPONSE, 1.4, PrivacySink.LOG, 1.2, PrivacySink.EXCEPTION, 1.1, PrivacySink.FILE, 1.0));
	static final double LATENT_FACTOR = 0.3;
	static final double SATURATION = 60;

	static final CitizenRisk.Weights WEIGHTS = new CitizenRisk.Weights(wire(SEVERITY, Severity::wireValue),
			wire(CATEGORY, PrivacyCategory::wireValue), wire(SINK, PrivacySink::wireValue), LATENT_FACTOR, SATURATION);

	private CitizenRiskScore() {
	}

	static boolean isFlow(FindingSummary f) {
		return f.privacy().type() == PrivacyType.FLOW;
	}

	static double weight(FindingSummary f) {
		double base = SEVERITY.getOrDefault(f.severity(), 1.0) * CATEGORY.getOrDefault(f.privacy().category(), 1.0);
		return isFlow(f) ? base * SINK.getOrDefault(f.privacy().sink(), 1.0) : base * LATENT_FACTOR;
	}

	static double sum(List<FindingSummary> privacyFindings) {
		return privacyFindings.stream().mapToDouble(CitizenRiskScore::weight).sum();
	}

	static int score(double sum) {
		return (int) Math.round(100 * (1 - Math.exp(-sum / SATURATION)));
	}

	static String level(int score) {
		if (score >= 75) {
			return "critical";
		}
		if (score >= 50) {
			return "high";
		}
		return score >= 25 ? "moderate" : "low";
	}

	private static <E extends Enum<E>> Map<String, Double> wire(Map<E, Double> weights, Function<E, String> wireValue) {
		Map<String, Double> out = new LinkedHashMap<>();
		weights.entrySet().stream()
			.sorted(Map.Entry.<E, Double>comparingByValue().reversed())
			.forEach(e -> out.put(wireValue.apply(e.getKey()), e.getValue()));
		return out;
	}
}

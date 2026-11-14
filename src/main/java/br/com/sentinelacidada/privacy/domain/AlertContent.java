package br.com.sentinelacidada.privacy.domain;

/** A citizen alert as stored (JSON). {@code severity} is the finding's severity when it was written. */
public record AlertContent(String title, String whatHappens, String whoIsAffected, String citizenGuidance,
		String technicalFix, String severity) {
}

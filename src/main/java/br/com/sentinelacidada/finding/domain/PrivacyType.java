package br.com.sentinelacidada.finding.domain;

import com.fasterxml.jackson.annotation.JsonValue;

/** {@code MAPPING}: where personal data lives; {@code FLOW}: personal data reaching an insecure output (taint). */
public enum PrivacyType {
	MAPPING, FLOW;

	@JsonValue
	public String wireValue() {
		return name().toLowerCase();
	}
}

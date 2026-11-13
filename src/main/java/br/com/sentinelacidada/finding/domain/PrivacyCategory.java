package br.com.sentinelacidada.finding.domain;

import com.fasterxml.jackson.annotation.JsonValue;

/** Kind of personal data a privacy finding is about. */
public enum PrivacyCategory {
	IDENTIFICATION, HEALTH, CREDENTIAL, CONTACT, FINANCIAL;

	@JsonValue
	public String wireValue() {
		return name().toLowerCase();
	}
}

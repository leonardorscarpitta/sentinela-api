package br.com.sentinelacidada.finding.domain;

import com.fasterxml.jackson.annotation.JsonValue;

/** Insecure output a privacy flow reaches. */
public enum PrivacySink {
	LOG("log"), HTTP_RESPONSE("httpResponse"), EXTERNAL_API("externalApi"), FILE("file"), EXCEPTION("exception");

	private final String wireValue;

	PrivacySink(String wireValue) {
		this.wireValue = wireValue;
	}

	@JsonValue
	public String wireValue() {
		return wireValue;
	}
}

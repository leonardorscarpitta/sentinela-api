package br.com.sentinelacidada.shared.domain;

import com.fasterxml.jackson.annotation.JsonValue;

public enum Phase {
	SOURCE, BUILD, DEPLOY;

	@JsonValue
	public String wireValue() {
		return name().toLowerCase();
	}
}

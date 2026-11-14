package br.com.sentinelacidada.project.domain;

import com.fasterxml.jackson.annotation.JsonValue;

/** Whether Sentinela can read a private repository, per the last access check. */
public enum AccessStatus {
	PENDING, VERIFIED, FAILED;

	@JsonValue
	public String code() {
		return name().toLowerCase();
	}
}

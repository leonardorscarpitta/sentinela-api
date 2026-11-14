package br.com.sentinelacidada.organization.service;

import java.time.Instant;
import java.util.UUID;

import br.com.sentinelacidada.organization.domain.Role;

/** {@code email} is null for callers that do not manage the team. */
public record MemberView(UUID id, String name, String email, Role role, boolean active, Instant lastActiveAt) {

	public MemberView withoutEmail() {
		return new MemberView(id, name, null, role, active, lastActiveAt);
	}
}

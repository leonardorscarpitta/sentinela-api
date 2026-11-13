package br.com.sentinelacidada.organization.service;

import java.time.Instant;
import java.util.UUID;

import br.com.sentinelacidada.organization.domain.Role;

/** An invitation addressed to the signed-in account. */
public record PendingInvitationView(UUID id, Organization organization, Role role, String invitedBy, Instant sentAt,
		Instant expiresAt) {

	public record Organization(String slug, String name) {
	}
}

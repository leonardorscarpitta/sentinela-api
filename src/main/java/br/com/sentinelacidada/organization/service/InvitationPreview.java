package br.com.sentinelacidada.organization.service;

import br.com.sentinelacidada.organization.domain.Role;

/** What an invitation link offers, shown before the user decides to join. */
public record InvitationPreview(String organizationName, String organizationSlug, Role role) {
}

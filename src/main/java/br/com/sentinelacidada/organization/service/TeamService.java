package br.com.sentinelacidada.organization.service;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import br.com.sentinelacidada.organization.domain.Permission;
import br.com.sentinelacidada.organization.domain.Role;

public interface TeamService {

	List<MemberView> findMembers(UUID organizationId);

	MemberView changeRole(Tenant tenant, UUID memberId, Role role);

	void removeMember(Tenant tenant, UUID memberId);

	/** The current owner becomes an admin. */
	List<MemberView> transferOwnership(Tenant tenant, UUID memberId);

	List<InvitationView> findInvitations(UUID organizationId);

	InvitationView invite(Tenant tenant, InviteCommand command);

	InvitationView resend(UUID organizationId, UUID invitationId);

	void revokeInvitation(UUID organizationId, UUID invitationId);

	List<RoleView> roles(Tenant tenant);

	/** Owners edit every role but their own; admins only analysts, developers and viewers. */
	RoleView updateRolePermissions(Tenant tenant, Role role, Set<Permission> permissions);
}

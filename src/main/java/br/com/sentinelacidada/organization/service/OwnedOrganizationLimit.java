package br.com.sentinelacidada.organization.service;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import br.com.sentinelacidada.organization.domain.Role;
import br.com.sentinelacidada.organization.persistence.MemberRepository;
import br.com.sentinelacidada.shared.error.ApiProblemException;

/** Each account can own one organization; organizations owned before this rule stay as they are. */
@Component
class OwnedOrganizationLimit {

	static final int MAX_OWNED = 1;

	private final MemberRepository members;
	private final JdbcClient jdbc;

	OwnedOrganizationLimit(MemberRepository members, JdbcClient jdbc) {
		this.members = members;
		this.jdbc = jdbc;
	}

	/**
	 * Before the account becomes the owner of a (new or existing) organization. Callers must be in a transaction: the
	 * per-account lock is held until it ends, so two concurrent requests cannot both pass the check.
	 */
	void requireRoomFor(String subject) {
		if (subject == null) {
			return;
		}
		jdbc.sql("SELECT pg_advisory_xact_lock(hashtext(:subject))").param("subject", subject).query().singleValue();
		if (members.countByUserSubjectAndRole(subject, Role.OWNER) >= MAX_OWNED) {
			throw new ApiProblemException(HttpStatus.CONFLICT, "organization-limit", "Each account can own one organization");
		}
	}
}

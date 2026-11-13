package br.com.sentinelacidada.organization.service;

import java.util.List;

import br.com.sentinelacidada.organization.domain.Feature;
import br.com.sentinelacidada.organization.domain.Permission;
import br.com.sentinelacidada.organization.domain.Role;

/** An organization the caller belongs to, with the caller's role there. {@code features} are the optional features enabled for it. */
public record OrganizationMembership(String slug, String name, Role role, List<Permission> permissions, List<Feature> features) {
}

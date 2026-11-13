package br.com.sentinelacidada.organization.service;

import java.util.Set;

import br.com.sentinelacidada.organization.domain.TokenScope;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record CreateApiTokenCommand(
		@NotBlank @Size(max = 100) String name,
		@NotEmpty Set<TokenScope> scopes,
		int expiresInDays) {
}

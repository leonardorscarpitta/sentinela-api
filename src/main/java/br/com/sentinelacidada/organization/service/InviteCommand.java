package br.com.sentinelacidada.organization.service;

import br.com.sentinelacidada.organization.domain.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record InviteCommand(@NotBlank @Email @Size(max = 254) String email, @NotNull Role role) {
}

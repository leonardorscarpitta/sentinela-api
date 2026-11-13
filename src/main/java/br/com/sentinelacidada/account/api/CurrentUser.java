package br.com.sentinelacidada.account.api;

/** {@code platformAdmin}: Sentinela administrator (realm role {@code sentinela-admin}); views every organization read-only and can delete organizations. */
public record CurrentUser(String username, String name, String email, boolean emailVerified, boolean platformAdmin) {
}

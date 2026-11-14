package br.com.sentinelacidada.scanjob.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;

import br.com.sentinelacidada.shared.config.SentinelaProperties;

/** {@code /internal/**} is only for the scanner runner: token auth, stateless, no CORS, never the OIDC chain. */
@Configuration
class InternalApiSecurityConfig {

	private static final Logger log = LoggerFactory.getLogger(InternalApiSecurityConfig.class);

	@Bean
	@Order(1)
	SecurityFilterChain internalApiChain(HttpSecurity http, SentinelaProperties properties) throws Exception {
		SentinelaProperties.Scanner scanner = properties.scanner();
		if (!scanner.enabled()) {
			log.warn("Scanner job API disabled: SCANNER_TOKEN must have at least {} characters.",
					SentinelaProperties.Scanner.MIN_TOKEN_LENGTH);
		}
		return http.securityMatcher("/internal/**")
			.csrf(csrf -> csrf.disable())
			.cors(cors -> cors.disable())
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.requestCache(cache -> cache.disable())
			.addFilterBefore(new ScannerTokenFilter(scanner), AuthorizationFilter.class)
			.authorizeHttpRequests(auth -> auth.anyRequest().hasRole(ScannerTokenFilter.ROLE))
			.build();
	}
}

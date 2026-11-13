package br.com.sentinelacidada.usage.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.tags.Tag;
import br.com.sentinelacidada.organization.api.WithoutOrganization;
import br.com.sentinelacidada.usage.service.UsageService;

/** Public (no sign-in): the limits every organization gets, shown on the home page, the guide and the Terms of Use. */
@RestController
@WithoutOrganization
@RequestMapping("/api/v1/limits")
@Tag(name = "Usage", description = "The organization's monthly scan quota and enabled features")
class LimitsController {

	record Limits(int scansPerMonth) {
	}

	private final UsageService usage;

	LimitsController(UsageService usage) {
		this.usage = usage;
	}

	@GetMapping
	Limits limits() {
		return new Limits(usage.monthlyScanLimit());
	}
}

package br.com.sentinelacidada.dashboard.api;

import java.time.ZoneId;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.sentinelacidada.dashboard.service.DashboardService;
import br.com.sentinelacidada.dashboard.service.DashboardSummary;
import br.com.sentinelacidada.dashboard.service.DashboardTrends;
import br.com.sentinelacidada.organization.service.Tenant;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/dashboard")
@Tag(name = "Dashboard", description = "Security overview across the organization's repositories")
class DashboardController {

	private final DashboardService dashboard;

	DashboardController(DashboardService dashboard) {
		this.dashboard = dashboard;
	}

	@GetMapping("/summary")
	DashboardSummary summary(Tenant tenant) {
		return dashboard.summary(tenant.organizationId());
	}

	@GetMapping("/trends")
	@Operation(summary = "Daily findings and pipeline history", description = "days: 30 or 90; tz: IANA time zone of the days (default UTC)")
	DashboardTrends trends(Tenant tenant, @RequestParam(defaultValue = "30") int days,
			@RequestParam(defaultValue = "UTC") String tz) {
		if (days != 30 && days != 90) {
			throw new IllegalArgumentException("days must be 30 or 90");
		}
		if (!ZoneId.getAvailableZoneIds().contains(tz)) {
			throw new IllegalArgumentException("Unknown time zone");
		}
		return dashboard.trends(tenant.organizationId(), days, ZoneId.of(tz));
	}
}

package br.com.sentinelacidada.report.service;

import java.util.List;
import java.util.Map;

import br.com.sentinelacidada.report.domain.ReportContent.AiNotice;
import br.com.sentinelacidada.report.domain.ReportContent.Dependencies;
import br.com.sentinelacidada.report.domain.ReportContent.Kpis;
import br.com.sentinelacidada.report.domain.ReportContent.Narrative;
import br.com.sentinelacidada.report.domain.ReportContent.OwaspCategory;
import br.com.sentinelacidada.report.domain.ReportContent.TopRisk;
import br.com.sentinelacidada.report.domain.ReportContent.TrendPoint;

/** {@code content} is {@code null} while the report is being generated. */
public record ReportDetails(ReportSummary report, Content content) {

	public record Content(String summary, Kpis kpis, List<TrendPoint> trend, Map<String, Integer> severity,
			List<TopRisk> topRisks, List<OwaspCategory> owasp, List<String> recommendations, Narrative narrative,
			AiNotice aiNotice, String textLanguage, Dependencies dependencies) {
	}
}

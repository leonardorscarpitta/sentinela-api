package br.com.sentinelacidada.privacy.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import br.com.sentinelacidada.finding.service.FindingDetails;
import br.com.sentinelacidada.finding.service.FindingSummary;
import br.com.sentinelacidada.privacy.domain.AlertContent;
import br.com.sentinelacidada.shared.ai.ClaudeClient;

/**
 * The only data Sentinela Cidadã sends to the AI provider to write a citizen alert: the language, the rule id, the
 * personal-data category, mapping/flow, the insecure output, severity, CWE, the rule's own title and the repository
 * stack. Never source code, file paths or lines, matched values, names, e-mails, repository or organization names.
 * Keep it that way: README "Citizen alerts" describes exactly this.
 */
final class CitizenAlertPayload {

	private static final int MAX_TITLE_IN = 100;
	/** Roughly 400 input and 250-350 output tokens per alert. */
	static final long MAX_TOKENS = 700;

	private static final int MAX_TITLE = 120;
	private static final int MAX_WHAT = 500;
	private static final int MAX_WHO = 300;
	private static final int MAX_GUIDANCE = 450;
	private static final int MAX_FIX = 300;

	private static final Map<String, Object> TEXT = Map.of("type", "string");
	static final Map<String, Object> RESPONSE_SCHEMA = Map.of("type", "object",
			"properties", Map.of("title", TEXT, "whatHappens", TEXT, "whoIsAffected", TEXT, "citizenGuidance", TEXT,
					"technicalFix", TEXT),
			"required", List.of("title", "whatHappens", "whoIsAffected", "citizenGuidance", "technicalFix"),
			"additionalProperties", false);

	static final String SYSTEM_INSTRUCTION = """
			You turn one privacy flaw found by static analysis in a public-service (CitizenTech) app into a short alert \
			for the citizens whose data the app handles, in Brazil (LGPD). Use only the JSON facts; never invent data, \
			numbers, names, incidents or laws beyond LGPD; do not say the data was already stolen: describe the risk.
			title: what can happen to the citizen's data, max 12 words, no jargon;
			whatHappens: what the app does with that kind of data and where it ends up, everyday words, max 45 words;
			whoIsAffected: which citizens are exposed (e.g. everyone who registers, patients in triage), max 25 words;
			citizenGuidance: what the citizen can do now (e.g. ask the service for information under LGPD art. 18, \
			be wary of calls or messages quoting their data), max 40 words;
			technicalFix: one concrete fix for the development team, max 30 words.
			category: identification = CPF, RG, CNS documents; health = diagnoses, symptoms, CID-10 (sensitive data, \
			LGPD art. 11); credential = passwords; contact = phone, e-mail, address; financial = card, bank, income.
			sink: log = internal logs; httpResponse = sent back to the app or browser; externalApi = sent to another \
			company; file = written to a file on the server; exception = error messages. type mapping = the data is \
			stored there, no leak found yet.
			Write in the language of "lang" (pt-BR Brazilian Portuguese, en-US English). Plain text, no markdown.""";

	private CitizenAlertPayload() {
	}

	/** One alert per rule, category and output: it describes the kind of leak, not one occurrence. */
	static String cacheKey(FindingSummary f) {
		return String.join("|", f.kind().wireValue(), f.ruleId(), f.privacy().category().wireValue(),
				f.privacy().sink() == null ? "" : f.privacy().sink().wireValue());
	}

	static Map<String, Object> of(FindingDetails details, String stack, String language) {
		FindingSummary f = details.finding();
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("lang", language);
		payload.put("ruleId", ClaudeClient.safeIdentifier(f.ruleId()));
		payload.put("category", f.privacy().category().wireValue());
		payload.put("type", f.privacy().type().wireValue());
		payload.put("sink", f.privacy().sink() == null ? null : f.privacy().sink().wireValue());
		payload.put("severity", f.severity().wireValue());
		payload.put("cwe", details.cwe() != null ? details.cwe() : f.cwe());
		payload.put("title", truncate(f.title()));
		payload.put("stack", stack);
		payload.values().removeIf(v -> v == null || "".equals(v));
		return payload;
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record Answer(String title, String whatHappens, String whoIsAffected, String citizenGuidance, String technicalFix) {
	}

	static Optional<AlertContent> validated(Answer a, String severity) {
		String title = ClaudeClient.clean(a.title(), MAX_TITLE);
		String what = ClaudeClient.clean(a.whatHappens(), MAX_WHAT);
		String who = ClaudeClient.clean(a.whoIsAffected(), MAX_WHO);
		String guidance = ClaudeClient.clean(a.citizenGuidance(), MAX_GUIDANCE);
		String fix = ClaudeClient.clean(a.technicalFix(), MAX_FIX);
		if (title == null || what == null || who == null || guidance == null || fix == null) {
			return Optional.empty();
		}
		return Optional.of(new AlertContent(title, what, who, guidance, fix, severity));
	}

	private static String truncate(String s) {
		if (s == null) {
			return null;
		}
		String t = s.strip().replaceAll("\\s+", " ");
		return t.length() <= MAX_TITLE_IN ? t : t.substring(0, MAX_TITLE_IN);
	}
}

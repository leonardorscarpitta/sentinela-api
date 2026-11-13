package io.github.artemiscore.artemisappsec.privacy.service;

import java.time.Instant;

/**
 * A privacy finding explained to citizens, in {@code language} (the organization's language when it was written).
 * {@code technicalFix} is the one line addressed to the development team.
 */
public record CitizenAlertView(String title, String whatHappens, String whoIsAffected, String citizenGuidance,
		String technicalFix, String severity, String language, String model, Instant createdAt) {
}

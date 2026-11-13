package br.com.sentinelacidada.privacy.domain;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** AI-written citizen alert, shared by every privacy finding of the organization with the same cache key. */
@Entity
@Table(name = "citizen_alert")
public class CitizenAlert {

	@Id
	private UUID id;

	@Column(name = "organization_id", nullable = false)
	private UUID organizationId;

	@Column(name = "cache_key", nullable = false)
	private String cacheKey;

	@Column(nullable = false)
	private String language;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(nullable = false)
	private AlertContent content;

	@Column(nullable = false)
	private String model;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected CitizenAlert() {
	}

	public CitizenAlert(UUID organizationId, String cacheKey, String language, AlertContent content, String model,
			Instant createdAt) {
		this.id = UUID.randomUUID();
		this.organizationId = organizationId;
		this.cacheKey = cacheKey;
		this.language = language;
		this.content = content;
		this.model = model;
		this.createdAt = createdAt;
	}

	public String getCacheKey() {
		return cacheKey;
	}

	public String getLanguage() {
		return language;
	}

	public AlertContent getContent() {
		return content;
	}

	public String getModel() {
		return model;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}

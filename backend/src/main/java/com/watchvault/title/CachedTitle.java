package com.watchvault.title;

import java.time.Instant;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.watchvault.media.MediaType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** One cached TMDB title (V3 migration). The details are stored as JSON in {@code payload}. */
@Entity
@Table(name = "title_cache")
public class CachedTitle {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(name = "media_type", nullable = false, length = 10)
	private MediaType mediaType;

	@Column(name = "tmdb_id", nullable = false)
	private int tmdbId;

	/** TitleDetails as JSON. Hibernate maps it to MySQL's JSON column type. */
	@JdbcTypeCode(SqlTypes.JSON)
	@Column(nullable = false)
	private String payload;

	@Column(name = "fetched_at", nullable = false)
	private Instant fetchedAt;

	/** Required by JPA. Not for application code. */
	protected CachedTitle() {
	}

	public CachedTitle(MediaType mediaType, int tmdbId, String payload, Instant fetchedAt) {
		this.mediaType = mediaType;
		this.tmdbId = tmdbId;
		this.payload = payload;
		this.fetchedAt = fetchedAt;
	}

	/** Replace with a newer copy from TMDB. */
	public void refresh(String newPayload, Instant newFetchedAt) {
		this.payload = newPayload;
		this.fetchedAt = newFetchedAt;
	}

	public MediaType getMediaType() {
		return mediaType;
	}

	public int getTmdbId() {
		return tmdbId;
	}

	public String getPayload() {
		return payload;
	}

	public Instant getFetchedAt() {
		return fetchedAt;
	}
}

package com.watchvault.user;

import java.time.Instant;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/**
 * A user's settings (1:1 with User). Shares the user's primary key via @MapsId,
 * matching user_preferences.user_id in the V1 migration.
 */
@Entity
@Table(name = "user_preferences")
public class UserPreferences {

	/** ISO 3166-1 country code. The app targets UK streaming availability. */
	public static final String DEFAULT_REGION = "GB";

	@Id
	private Long userId;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@MapsId
	@JoinColumn(name = "user_id")
	private User user;

	// Hibernate writes every column on insert, so the DB default never applies; this one does.
	@Column(nullable = false, length = 2)
	private String region = DEFAULT_REGION;

	/** ISO 639-1 codes, as TMDB's original_language uses them ("en", "ja"). */
	@ElementCollection
	@CollectionTable(name = "user_preferred_languages", joinColumns = @JoinColumn(name = "user_id"))
	@Column(name = "language_code", nullable = false, length = 2)
	private Set<String> preferredLanguages = new HashSet<>();

	@ElementCollection
	@CollectionTable(name = "user_preferred_genres", joinColumns = @JoinColumn(name = "user_id"))
	private Set<PreferredGenre> preferredGenres = new HashSet<>();

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	/** Required by JPA. Not for application code. */
	protected UserPreferences() {
	}

	public UserPreferences(User user) {
		this.user = user;
	}

	public void changeRegion(String region) {
		this.region = region.toUpperCase(Locale.ROOT);
	}

	public void replaceLanguages(Set<String> languageCodes) {
		preferredLanguages.clear();
		languageCodes.forEach(code -> preferredLanguages.add(code.toLowerCase(Locale.ROOT)));
	}

	public void replaceGenres(Set<PreferredGenre> genres) {
		preferredGenres.clear();
		preferredGenres.addAll(genres);
	}

	public Long getUserId() {
		return userId;
	}

	public String getRegion() {
		return region;
	}

	/** Read-only copy; change through replaceLanguages. */
	public Set<String> getPreferredLanguages() {
		return Set.copyOf(preferredLanguages);
	}

	/** Read-only copy; change through replaceGenres. */
	public Set<PreferredGenre> getPreferredGenres() {
		return Set.copyOf(preferredGenres);
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}
}

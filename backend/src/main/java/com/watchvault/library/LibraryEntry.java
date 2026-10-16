package com.watchvault.library;

import java.time.Instant;
import java.time.LocalDate;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
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

/**
 * One title in one user's library (V4 migration). Holds only user data plus the TMDB id;
 * the title itself is looked up in the TMDB cache.
 */
@Entity
@Table(name = "library_entries")
public class LibraryEntry {

	public static final int MIN_RATING = 1;
	public static final int MAX_RATING = 10;
	public static final int MAX_NOTES_LENGTH = 1000;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** Owner. Every query filters on this, so one user can never reach another's entries. */
	@Column(name = "user_id", nullable = false, updatable = false)
	private Long userId;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(name = "media_type", nullable = false, length = 10, updatable = false)
	private MediaType mediaType;

	@Column(name = "tmdb_id", nullable = false, updatable = false)
	private int tmdbId;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(nullable = false, length = 20)
	private LibraryStatus status;

	/** 1-10, or null when not rated. TINYINT in MySQL. */
	@JdbcTypeCode(SqlTypes.TINYINT)
	private Integer rating;

	/** Private notes, user-written: validated on the way in, escaped by React on the way out. */
	@Column(length = MAX_NOTES_LENGTH)
	private String notes;

	@Column(name = "completed_on")
	private LocalDate completedOn;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	/** Required by JPA. Not for application code. */
	protected LibraryEntry() {
	}

	public LibraryEntry(Long userId, MediaType mediaType, int tmdbId) {
		this.userId = userId;
		this.mediaType = mediaType;
		this.tmdbId = tmdbId;
	}

	/**
	 * Applies the user's choices. completedOn records when the title was finished: set on the
	 * day it becomes COMPLETED (kept if it already was), cleared if it moves to another status.
	 */
	public void update(LibraryStatus newStatus, Integer newRating, String newNotes, LocalDate today) {
		if (newStatus == LibraryStatus.COMPLETED) {
			if (status != LibraryStatus.COMPLETED || completedOn == null) {
				completedOn = today;
			}
		} else {
			completedOn = null;
		}
		this.status = newStatus;
		this.rating = newRating;
		this.notes = newNotes;
	}

	public Long getId() {
		return id;
	}

	public Long getUserId() {
		return userId;
	}

	public MediaType getMediaType() {
		return mediaType;
	}

	public int getTmdbId() {
		return tmdbId;
	}

	public LibraryStatus getStatus() {
		return status;
	}

	public Integer getRating() {
		return rating;
	}

	public String getNotes() {
		return notes;
	}

	public LocalDate getCompletedOn() {
		return completedOn;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}
}

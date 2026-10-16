package com.watchvault.user;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.watchvault.media.MediaType;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

/**
 * One preferred genre: a TMDB genre ID plus the media type it belongs to
 * (movie and TV genre IDs differ). Stored as a row in user_preferred_genres.
 */
@Embeddable
public record PreferredGenre(

		@Enumerated(EnumType.STRING)
		@JdbcTypeCode(SqlTypes.VARCHAR)
		@Column(name = "media_type", nullable = false, length = 10)
		MediaType mediaType,

		@Column(name = "genre_id", nullable = false)
		int genreId) {
}

package com.watchvault.user;

import com.watchvault.media.MediaType;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** A preferred genre as the API sends and receives it (a DTO, kept apart from the entity's PreferredGenre). */
public record GenreChoice(

		@NotNull(message = "Media type is required (MOVIE or TV).")
		MediaType mediaType,

		@NotNull(message = "Genre id is required.")
		@Positive(message = "Genre id must be positive.")
		Integer genreId) {

	static GenreChoice from(PreferredGenre genre) {
		return new GenreChoice(genre.mediaType(), genre.genreId());
	}

	PreferredGenre toPreferredGenre() {
		return new PreferredGenre(mediaType, genreId);
	}
}

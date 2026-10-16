package com.watchvault.user;

import java.util.Set;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Replaces all preferences at once (PUT semantics). The shape is checked here;
 * that the codes really exist is checked in UserPreferencesService.
 */
public record UpdatePreferencesRequest(

		@NotNull(message = "Region is required.")
		@Pattern(regexp = "[A-Z]{2}", message = "Region must be a 2-letter country code, e.g. GB.")
		String region,

		@NotNull(message = "Languages are required (send [] for none).")
		@Size(max = MAX_LANGUAGES, message = "Choose at most 20 languages.")
		Set<@NotNull @Pattern(regexp = "[a-z]{2}", message = "Languages must be 2-letter codes, e.g. en.") String> languages,

		@NotNull(message = "Genres are required (send [] for none).")
		@Size(max = MAX_GENRES, message = "Choose at most 50 genres.")
		Set<@NotNull @Valid GenreChoice> genres) {

	static final int MAX_LANGUAGES = 20;
	static final int MAX_GENRES = 50;
}

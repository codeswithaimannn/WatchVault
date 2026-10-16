package com.watchvault.user;

import java.util.Set;
import java.util.stream.Collectors;

public record PreferencesResponse(String region, Set<String> languages, Set<GenreChoice> genres) {

	static PreferencesResponse from(UserPreferences preferences) {
		Set<GenreChoice> genres = preferences.getPreferredGenres().stream()
				.map(GenreChoice::from)
				.collect(Collectors.toSet());
		return new PreferencesResponse(preferences.getRegion(), preferences.getPreferredLanguages(), genres);
	}
}

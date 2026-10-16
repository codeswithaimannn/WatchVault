package com.watchvault.user;

import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.watchvault.common.InvalidInputException;

/**
 * Reads and replaces a user's preferences. Genre ids are only checked for shape here;
 * checking them against TMDB's genre list comes with the API adapters (Milestone 2).
 */
@Service
public class UserPreferencesService {

	private static final Set<String> COUNTRY_CODES = Set.of(Locale.getISOCountries());
	private static final Set<String> LANGUAGE_CODES = Set.of(Locale.getISOLanguages());

	private final UserPreferencesRepository preferences;

	public UserPreferencesService(UserPreferencesRepository preferences) {
		this.preferences = preferences;
	}

	@Transactional(readOnly = true)
	public PreferencesResponse get(Long userId) {
		return PreferencesResponse.from(load(userId));
	}

	@Transactional
	public PreferencesResponse update(Long userId, UpdatePreferencesRequest request) {
		checkCodesExist(request);
		UserPreferences userPreferences = load(userId);
		userPreferences.changeRegion(request.region());
		userPreferences.replaceLanguages(request.languages());
		userPreferences.replaceGenres(request.genres().stream()
				.map(GenreChoice::toPreferredGenre)
				.collect(Collectors.toSet()));
		return PreferencesResponse.from(userPreferences);
	}

	/** Every account gets preferences at registration, so a missing row means the account is gone. */
	private UserPreferences load(Long userId) {
		return preferences.findById(userId).orElseThrow(AccountNotFoundException::new);
	}

	private static void checkCodesExist(UpdatePreferencesRequest request) {
		if (!COUNTRY_CODES.contains(request.region())) {
			throw new InvalidInputException("Unknown region code: " + request.region());
		}
		for (String language : request.languages()) {
			if (!LANGUAGE_CODES.contains(language)) {
				throw new InvalidInputException("Unknown language code: " + language);
			}
		}
	}
}

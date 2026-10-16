package com.watchvault.title;

import java.util.Collection;

/**
 * TMDB has no "Anime" genre. Our rule (DECISIONS.md, M0 fact 6):
 * anime = Animation genre AND Japanese original language.
 */
public final class AnimeRule {

	/** TMDB's Animation genre id; the same for movies and TV. */
	public static final int ANIMATION_GENRE_ID = 16;
	public static final String JAPANESE = "ja";

	private AnimeRule() {
	}

	public static boolean isAnime(Collection<Integer> genreIds, String originalLanguage) {
		return genreIds.contains(ANIMATION_GENRE_ID) && JAPANESE.equals(originalLanguage);
	}
}

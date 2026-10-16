package com.watchvault.title;

import java.time.LocalDate;
import java.util.List;

/** One season of a TV show with its episodes. */
public record SeasonDetails(int tvTmdbId, int seasonNumber, String name, LocalDate airDate, List<Episode> episodes) {

	/**
	 * @param runtimeMinutes TMDB's per-episode runtime, or null if unknown. An estimate, not recorded viewing time.
	 * @param airDate        null if not announced
	 */
	public record Episode(int episodeNumber, String name, String overview, LocalDate airDate,
			Integer runtimeMinutes, String stillPath) {
	}
}

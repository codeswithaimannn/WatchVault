package com.watchvault.title;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import com.watchvault.media.MediaType;

/**
 * Full details of a movie or TV show, as cached from TMDB.
 *
 * @param releaseDate     TMDB's main date (movie premiere / first air date). Not necessarily the UK date.
 * @param ukReleaseDate   movies only: first UK cinema release, or null if TMDB has none. Never guessed.
 * @param runtimeMinutes  movies only; TV runtimes come per episode (show-level runtime is often empty)
 * @param seasons         TV only (empty for movies), including "Specials" as season 0
 * @param fetchedAt       when this copy came from TMDB (cache freshness; shown if the API is down)
 */
public record TitleDetails(
		MediaType mediaType,
		int tmdbId,
		String title,
		String originalTitle,
		String originalLanguage,
		String overview,
		String tagline,
		String posterPath,
		String backdropPath,
		LocalDate releaseDate,
		LocalDate ukReleaseDate,
		Integer runtimeMinutes,
		String status,
		List<Genre> genres,
		String imdbId,
		Integer tvdbId,
		boolean anime,
		List<SeasonSummary> seasons,
		Instant fetchedAt) {

	public record SeasonSummary(int seasonNumber, String name, int episodeCount, LocalDate airDate) {
	}
}

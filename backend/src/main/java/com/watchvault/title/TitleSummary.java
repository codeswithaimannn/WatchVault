package com.watchvault.title;

import java.time.LocalDate;
import java.util.List;

import com.watchvault.media.MediaType;

/**
 * One search result (movie or TV show).
 *
 * @param releaseDate main release / first air date; null when TMDB doesn't know it
 * @param posterPath  TMDB image path like "/abc.jpg", or null; the frontend adds the image host and size
 */
public record TitleSummary(
		MediaType mediaType,
		int tmdbId,
		String title,
		String originalLanguage,
		String overview,
		String posterPath,
		LocalDate releaseDate,
		List<Integer> genreIds,
		boolean anime) {
}

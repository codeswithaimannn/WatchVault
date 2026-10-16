package com.watchvault.title;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.watchvault.media.MediaTypePaths;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * Film/TV metadata for the frontend. Everything goes through TitleCatalogService (with its caches);
 * the browser never talks to TMDB. Login required (SecurityConfig: any other request is authenticated).
 */
@RestController
@RequestMapping("/api")
public class TitleController {

	private static final int MAX_QUERY_LENGTH = 100;
	private static final int MAX_SEARCH_PAGE = 500;

	private final TitleCatalogService catalog;

	public TitleController(TitleCatalogService catalog) {
		this.catalog = catalog;
	}

	@GetMapping("/genres/{mediaType}")
	public List<Genre> genres(@PathVariable String mediaType) {
		return catalog.genres(MediaTypePaths.fromPath(mediaType));
	}

	@GetMapping("/search")
	public SearchResults search(
			@RequestParam @NotBlank @Size(max = MAX_QUERY_LENGTH) String q,
			@RequestParam(defaultValue = "1") @Min(1) @Max(MAX_SEARCH_PAGE) int page) {
		// "  the   matrix " and "The Matrix" become the same search (and the same cache entry).
		String query = q.trim().replaceAll("\\s+", " ");
		return catalog.search(query, page);
	}

	@GetMapping("/titles/{mediaType}/{tmdbId}")
	public TitleDetails details(@PathVariable String mediaType, @PathVariable @Positive int tmdbId) {
		return catalog.details(MediaTypePaths.fromPath(mediaType), tmdbId);
	}

	@GetMapping("/titles/{mediaType}/{tmdbId}/providers")
	public WatchProviders providers(@PathVariable String mediaType, @PathVariable @Positive int tmdbId) {
		return catalog.watchProviders(MediaTypePaths.fromPath(mediaType), tmdbId);
	}

	@GetMapping("/titles/tv/{tmdbId}/seasons/{seasonNumber}")
	public SeasonDetails season(@PathVariable @Positive int tmdbId, @PathVariable @PositiveOrZero int seasonNumber) {
		return catalog.season(tmdbId, seasonNumber);
	}
}

package com.watchvault.external.tmdb;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * TMDB's JSON shapes, only the fields we use. Package-private: nothing outside the adapter
 * sees TMDB's format, so a TMDB change touches only this package.
 * Dates stay Strings here because TMDB sometimes sends "" for "unknown".
 */
final class TmdbResponses {

	private TmdbResponses() {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record GenreList(List<GenreItem> genres) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record GenreItem(int id, String name) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record SearchPage(
			int page,
			@JsonProperty("total_pages") int totalPages,
			List<SearchItem> results) {
	}

	/** Multi-search mixes movies ("title") and TV ("name"); media_type says which. */
	@JsonIgnoreProperties(ignoreUnknown = true)
	record SearchItem(
			int id,
			@JsonProperty("media_type") String mediaType,
			String title,
			String name,
			@JsonProperty("original_language") String originalLanguage,
			String overview,
			@JsonProperty("poster_path") String posterPath,
			@JsonProperty("release_date") String releaseDate,
			@JsonProperty("first_air_date") String firstAirDate,
			@JsonProperty("genre_ids") List<Integer> genreIds) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record MovieDetails(
			int id,
			String title,
			@JsonProperty("original_title") String originalTitle,
			@JsonProperty("original_language") String originalLanguage,
			String overview,
			String tagline,
			@JsonProperty("poster_path") String posterPath,
			@JsonProperty("backdrop_path") String backdropPath,
			@JsonProperty("release_date") String releaseDate,
			Integer runtime,
			String status,
			@JsonProperty("imdb_id") String imdbId,
			List<GenreItem> genres,
			@JsonProperty("release_dates") ReleaseDates releaseDates) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record ReleaseDates(List<CountryReleases> results) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record CountryReleases(
			@JsonProperty("iso_3166_1") String country,
			@JsonProperty("release_dates") List<ReleaseDate> releaseDates) {
	}

	/** type: 1 premiere, 2 limited cinema, 3 cinema, 4 digital, 5 physical, 6 TV. */
	@JsonIgnoreProperties(ignoreUnknown = true)
	record ReleaseDate(@JsonProperty("release_date") String releaseDate, int type) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record TvDetails(
			int id,
			String name,
			@JsonProperty("original_name") String originalName,
			@JsonProperty("original_language") String originalLanguage,
			String overview,
			String tagline,
			@JsonProperty("poster_path") String posterPath,
			@JsonProperty("backdrop_path") String backdropPath,
			@JsonProperty("first_air_date") String firstAirDate,
			String status,
			List<GenreItem> genres,
			List<SeasonItem> seasons,
			@JsonProperty("external_ids") ExternalIds externalIds) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record SeasonItem(
			@JsonProperty("season_number") int seasonNumber,
			String name,
			@JsonProperty("episode_count") int episodeCount,
			@JsonProperty("air_date") String airDate) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record ExternalIds(@JsonProperty("imdb_id") String imdbId, @JsonProperty("tvdb_id") Integer tvdbId) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record Season(
			@JsonProperty("season_number") int seasonNumber,
			String name,
			@JsonProperty("air_date") String airDate,
			List<EpisodeItem> episodes) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record EpisodeItem(
			@JsonProperty("episode_number") int episodeNumber,
			String name,
			String overview,
			@JsonProperty("air_date") String airDate,
			Integer runtime,
			@JsonProperty("still_path") String stillPath) {
	}

	/** Keyed by country code ("GB", "US", ...). There is no region filter, so we pick ours. */
	@JsonIgnoreProperties(ignoreUnknown = true)
	record WatchProviderResults(Map<String, CountryProviders> results) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record CountryProviders(
			String link,
			List<ProviderItem> flatrate,
			List<ProviderItem> rent,
			List<ProviderItem> buy) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record ProviderItem(
			@JsonProperty("provider_id") int providerId,
			@JsonProperty("provider_name") String providerName,
			@JsonProperty("logo_path") String logoPath,
			@JsonProperty("display_priority") int displayPriority) {
	}
}

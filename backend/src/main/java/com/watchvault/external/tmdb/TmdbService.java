package com.watchvault.external.tmdb;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.watchvault.external.tmdb.TmdbResponses.CountryProviders;
import com.watchvault.external.tmdb.TmdbResponses.CountryReleases;
import com.watchvault.external.tmdb.TmdbResponses.GenreItem;
import com.watchvault.external.tmdb.TmdbResponses.GenreList;
import com.watchvault.external.tmdb.TmdbResponses.MovieDetails;
import com.watchvault.external.tmdb.TmdbResponses.ProviderItem;
import com.watchvault.external.tmdb.TmdbResponses.ReleaseDates;
import com.watchvault.external.tmdb.TmdbResponses.Season;
import com.watchvault.external.tmdb.TmdbResponses.SearchItem;
import com.watchvault.external.tmdb.TmdbResponses.SearchPage;
import com.watchvault.external.tmdb.TmdbResponses.TvDetails;
import com.watchvault.external.tmdb.TmdbResponses.WatchProviderResults;
import com.watchvault.media.MediaType;
import com.watchvault.title.AnimeRule;
import com.watchvault.title.Genre;
import com.watchvault.title.SearchResults;
import com.watchvault.title.SeasonDetails;
import com.watchvault.title.TitleDetails;
import com.watchvault.title.TitleSummary;
import com.watchvault.title.WatchProviders;

/** Fetches from TMDB and translates TMDB's shapes into our own DTOs. No caching here. */
@Service
public class TmdbService {

	/** TMDB refuses search pages above 500. */
	static final int MAX_SEARCH_PAGE = 500;

	/** release_dates "type" values that mean a cinema release (2 = limited, 3 = wide). */
	private static final List<Integer> CINEMA_RELEASE_TYPES = List.of(2, 3);

	/** Length of "yyyy-MM-dd" at the start of TMDB's release timestamps. */
	private static final int ISO_DATE_LENGTH = 10;

	private final TmdbClient client;

	public TmdbService(TmdbClient client) {
		this.client = client;
	}

	public List<Genre> genres(MediaType mediaType) {
		GenreList list = client.get("/genre/" + pathSegment(mediaType) + "/list", Map.of(), GenreList.class);
		return list.genres().stream().map(TmdbService::toGenre).toList();
	}

	public SearchResults search(String query, int page) {
		int safePage = Math.clamp(page, 1, MAX_SEARCH_PAGE);
		SearchPage raw = client.get("/search/multi",
				Map.of("query", query, "page", safePage, "include_adult", false), SearchPage.class);
		List<TitleSummary> titles = raw.results().stream()
				.map(TmdbService::toSummary)
				.flatMap(Optional::stream) // drops people and unknown types
				.toList();
		return new SearchResults(raw.page(), raw.totalPages(), titles);
	}

	/** One request: details plus release dates (append_to_response). */
	public TitleDetails movie(int tmdbId) {
		MovieDetails raw = client.get("/movie/" + tmdbId, Map.of("append_to_response", "release_dates"),
				MovieDetails.class);
		List<Genre> genres = toGenres(raw.genres());
		return new TitleDetails(MediaType.MOVIE, raw.id(), raw.title(), raw.originalTitle(), raw.originalLanguage(),
				raw.overview(), blankToNull(raw.tagline()), raw.posterPath(), raw.backdropPath(),
				parseDate(raw.releaseDate()), ukCinemaRelease(raw.releaseDates()), raw.runtime(),
				raw.status(), genres, blankToNull(raw.imdbId()), null,
				AnimeRule.isAnime(genreIds(genres), raw.originalLanguage()), List.of(), Instant.now());
	}

	/** One request: details plus IMDb/TVDB ids (used to link the show to TVmaze). */
	public TitleDetails tv(int tmdbId) {
		TvDetails raw = client.get("/tv/" + tmdbId, Map.of("append_to_response", "external_ids"), TvDetails.class);
		List<Genre> genres = toGenres(raw.genres());
		List<TitleDetails.SeasonSummary> seasons = Objects.requireNonNullElse(raw.seasons(), List.<TmdbResponses.SeasonItem>of())
				.stream()
				.map(s -> new TitleDetails.SeasonSummary(s.seasonNumber(), s.name(), s.episodeCount(), parseDate(s.airDate())))
				.toList();
		String imdbId = raw.externalIds() == null ? null : blankToNull(raw.externalIds().imdbId());
		Integer tvdbId = raw.externalIds() == null ? null : raw.externalIds().tvdbId();
		return new TitleDetails(MediaType.TV, raw.id(), raw.name(), raw.originalName(), raw.originalLanguage(),
				raw.overview(), blankToNull(raw.tagline()), raw.posterPath(), raw.backdropPath(),
				parseDate(raw.firstAirDate()), null, null, raw.status(), genres, imdbId, tvdbId,
				AnimeRule.isAnime(genreIds(genres), raw.originalLanguage()), seasons, Instant.now());
	}

	public SeasonDetails season(int tvTmdbId, int seasonNumber) {
		Season raw = client.get("/tv/" + tvTmdbId + "/season/" + seasonNumber, Map.of(), Season.class);
		List<SeasonDetails.Episode> episodes = Objects.requireNonNullElse(raw.episodes(), List.<TmdbResponses.EpisodeItem>of())
				.stream()
				.map(e -> new SeasonDetails.Episode(e.episodeNumber(), e.name(), e.overview(),
						parseDate(e.airDate()), e.runtime(), e.stillPath()))
				.toList();
		return new SeasonDetails(tvTmdbId, raw.seasonNumber(), raw.name(), parseDate(raw.airDate()), episodes);
	}

	/** Providers for our region only. A title not available here gets empty lists, not an error. */
	public WatchProviders watchProviders(MediaType mediaType, int tmdbId) {
		WatchProviderResults raw = client.get("/" + pathSegment(mediaType) + "/" + tmdbId + "/watch/providers",
				Map.of(), WatchProviderResults.class);
		String region = client.region();
		CountryProviders ours = raw.results() == null ? null : raw.results().get(region);
		if (ours == null) {
			return new WatchProviders(region, null, List.of(), List.of(), List.of(), WatchProviders.JUSTWATCH_ATTRIBUTION);
		}
		return new WatchProviders(region, ours.link(), toProviders(ours.flatrate()), toProviders(ours.rent()),
				toProviders(ours.buy()), WatchProviders.JUSTWATCH_ATTRIBUTION);
	}

	private static Optional<TitleSummary> toSummary(SearchItem item) {
		MediaType mediaType = switch (Objects.requireNonNullElse(item.mediaType(), "")) {
			case "movie" -> MediaType.MOVIE;
			case "tv" -> MediaType.TV;
			default -> null;
		};
		if (mediaType == null) {
			return Optional.empty();
		}
		boolean isMovie = mediaType == MediaType.MOVIE;
		List<Integer> genreIds = Objects.requireNonNullElse(item.genreIds(), List.of());
		return Optional.of(new TitleSummary(mediaType, item.id(),
				isMovie ? item.title() : item.name(),
				item.originalLanguage(), item.overview(), item.posterPath(),
				parseDate(isMovie ? item.releaseDate() : item.firstAirDate()),
				genreIds, AnimeRule.isAnime(genreIds, item.originalLanguage())));
	}

	/** Earliest cinema release in our region, or null. We never substitute another country's date. */
	private LocalDate ukCinemaRelease(ReleaseDates releaseDates) {
		if (releaseDates == null || releaseDates.results() == null) {
			return null;
		}
		return releaseDates.results().stream()
				.filter(country -> client.region().equals(country.country()))
				.map(CountryReleases::releaseDates)
				.filter(Objects::nonNull)
				.flatMap(List::stream)
				.filter(release -> CINEMA_RELEASE_TYPES.contains(release.type()))
				.map(release -> parseDate(release.releaseDate()))
				.filter(Objects::nonNull)
				.min(Comparator.naturalOrder())
				.orElse(null);
	}

	private static List<WatchProviders.Provider> toProviders(List<ProviderItem> items) {
		if (items == null) {
			return List.of();
		}
		return items.stream()
				.sorted(Comparator.comparingInt(ProviderItem::displayPriority))
				.map(p -> new WatchProviders.Provider(p.providerId(), p.providerName(), p.logoPath()))
				.toList();
	}

	private static List<Genre> toGenres(List<GenreItem> items) {
		return Objects.requireNonNullElse(items, List.<GenreItem>of()).stream().map(TmdbService::toGenre).toList();
	}

	private static Genre toGenre(GenreItem item) {
		return new Genre(item.id(), item.name());
	}

	private static List<Integer> genreIds(List<Genre> genres) {
		return genres.stream().map(Genre::id).toList();
	}

	/**
	 * TMDB dates come as "2024-02-27", as a timestamp "2024-03-01T00:00:00.000Z", as "" or null.
	 * Unknown or malformed means null: we never show a date we are not sure of.
	 */
	static LocalDate parseDate(String value) {
		if (value == null || value.length() < ISO_DATE_LENGTH) {
			return null;
		}
		try {
			return LocalDate.parse(value.substring(0, ISO_DATE_LENGTH));
		} catch (DateTimeParseException e) {
			return null;
		}
	}

	private static String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value;
	}

	private static String pathSegment(MediaType mediaType) {
		return mediaType == MediaType.MOVIE ? "movie" : "tv";
	}
}

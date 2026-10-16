package com.watchvault.external.tmdb;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.watchvault.external.ExternalCalls;
import com.watchvault.external.HttpProperties;
import com.watchvault.media.MediaType;
import com.watchvault.support.Fixtures;
import com.watchvault.title.Genre;
import com.watchvault.title.SearchResults;
import com.watchvault.title.SeasonDetails;
import com.watchvault.title.TitleDetails;
import com.watchvault.title.TitleSummary;
import com.watchvault.title.WatchProviders;

/** TmdbService mapping, against WireMock serving hand-written fixtures (not real TMDB data). */
class TmdbServiceTest {

	@RegisterExtension
	static final WireMockExtension tmdb = WireMockExtension.newInstance()
			.options(wireMockConfig().dynamicPort())
			.build();

	private TmdbService service;

	@BeforeEach
	void setUp() {
		HttpProperties http = new HttpProperties(Duration.ofSeconds(1), Duration.ofSeconds(2), 0, Duration.ofMillis(10));
		TmdbProperties properties = new TmdbProperties(tmdb.baseUrl(), "test-token", "en-GB", "GB");
		service = new TmdbService(new TmdbClient(properties, http, new ExternalCalls(http)));
	}

	@Test
	void genresAreFetchedPerMediaType() {
		tmdb.stubFor(get(urlPathEqualTo("/genre/tv/list"))
				.willReturn(okJson("{\"genres\": [{\"id\": 10765, \"name\": \"Sci-Fi & Fantasy\"}]}")));

		assertThat(service.genres(MediaType.TV)).containsExactly(new Genre(10765, "Sci-Fi & Fantasy"));
	}

	@Test
	void searchKeepsMoviesAndShowsAndDropsPeople() {
		stub("/search/multi", "search_multi.json");

		SearchResults results = service.search("example", 1);

		assertThat(results.page()).isEqualTo(1);
		assertThat(results.totalPages()).isEqualTo(3);
		assertThat(results.results()).extracting(TitleSummary::title)
				.containsExactly("Example Movie", "Example Anime Show", "Undated Show");
		tmdb.verify(getRequestedFor(urlPathEqualTo("/search/multi"))
				.withQueryParam("query", equalTo("example"))
				.withQueryParam("include_adult", equalTo("false")));
	}

	@Test
	void searchMapsMovieAndTvFieldsAndTheAnimeRule() {
		stub("/search/multi", "search_multi.json");

		SearchResults results = service.search("example", 1);

		TitleSummary movie = results.results().get(0);
		assertThat(movie.mediaType()).isEqualTo(MediaType.MOVIE);
		assertThat(movie.releaseDate()).isEqualTo(LocalDate.of(2023, 5, 1));
		assertThat(movie.anime()).isFalse();

		TitleSummary anime = results.results().get(1);
		assertThat(anime.mediaType()).isEqualTo(MediaType.TV);
		assertThat(anime.releaseDate()).isEqualTo(LocalDate.of(2020, 4, 4));
		assertThat(anime.anime()).isTrue(); // Animation + Japanese

		TitleSummary undated = results.results().get(2);
		assertThat(undated.releaseDate()).isNull(); // TMDB sent "", we never invent a date
		assertThat(undated.anime()).isFalse(); // Animation but English: not anime
	}

	@Test
	void searchPageIsKeptWithinTmdbLimits() {
		stub("/search/multi", "search_multi.json");

		service.search("example", 9999);

		tmdb.verify(getRequestedFor(urlPathEqualTo("/search/multi"))
				.withQueryParam("page", equalTo(String.valueOf(TmdbService.MAX_SEARCH_PAGE))));
	}

	@Test
	void movieDetailsUseOneRequestAndPickTheFirstUkCinemaRelease() {
		stub("/movie/1001", "movie_details.json");

		TitleDetails movie = service.movie(1001);

		assertThat(movie.title()).isEqualTo("Example Movie");
		assertThat(movie.runtimeMinutes()).isEqualTo(118);
		assertThat(movie.imdbId()).isEqualTo("tt0000001");
		assertThat(movie.releaseDate()).isEqualTo(LocalDate.of(2023, 5, 1)); // TMDB's main (US) date
		// UK: limited cinema 10 May beats wide 12 May; premiere (type 1) and digital (type 4) don't count.
		assertThat(movie.ukReleaseDate()).isEqualTo(LocalDate.of(2023, 5, 10));
		assertThat(movie.tagline()).isNull(); // "" becomes null
		assertThat(movie.seasons()).isEmpty();
		tmdb.verify(getRequestedFor(urlPathEqualTo("/movie/1001"))
				.withQueryParam("append_to_response", equalTo("release_dates")));
	}

	@Test
	void movieWithoutUkReleaseHasNoUkDate() {
		tmdb.stubFor(get(urlPathEqualTo("/movie/1002")).willReturn(okJson("""
				{"id": 1002, "title": "US Only", "original_language": "en", "genres": [],
				 "release_dates": {"results": [{"iso_3166_1": "US",
				   "release_dates": [{"release_date": "2023-01-01T00:00:00.000Z", "type": 3}]}]}}""")));

		assertThat(service.movie(1002).ukReleaseDate()).isNull();
	}

	@Test
	void tvDetailsIncludeExternalIdsAndSeasons() {
		stub("/tv/2001", "tv_details.json");

		TitleDetails show = service.tv(2001);

		assertThat(show.mediaType()).isEqualTo(MediaType.TV);
		assertThat(show.imdbId()).isEqualTo("tt0000002");
		assertThat(show.tvdbId()).isEqualTo(300001);
		assertThat(show.anime()).isTrue();
		assertThat(show.runtimeMinutes()).isNull(); // TV runtime comes per episode
		assertThat(show.seasons()).extracting(TitleDetails.SeasonSummary::seasonNumber).containsExactly(0, 1);
		assertThat(show.seasons().get(0).airDate()).isNull();
		tmdb.verify(getRequestedFor(urlPathEqualTo("/tv/2001"))
				.withQueryParam("append_to_response", equalTo("external_ids")));
	}

	@Test
	void seasonHasPerEpisodeRuntimesAndKeepsUnknownsAsNull() {
		stub("/tv/2001/season/1", "tv_season.json");

		SeasonDetails season = service.season(2001, 1);

		assertThat(season.episodes()).hasSize(2);
		assertThat(season.episodes().get(0).runtimeMinutes()).isEqualTo(24);
		SeasonDetails.Episode unannounced = season.episodes().get(1);
		assertThat(unannounced.runtimeMinutes()).isNull();
		assertThat(unannounced.airDate()).isNull();
	}

	@Test
	void watchProvidersAreForOurRegionInDisplayOrderWithJustWatchCredit() {
		stub("/movie/1001/watch/providers", "watch_providers.json");

		WatchProviders providers = service.watchProviders(MediaType.MOVIE, 1001);

		assertThat(providers.region()).isEqualTo("GB");
		assertThat(providers.link()).contains("locale=GB");
		assertThat(providers.stream()).extracting(WatchProviders.Provider::name)
				.containsExactly("First Service", "Second Service"); // sorted by display_priority
		assertThat(providers.buy()).hasSize(1);
		assertThat(providers.rent()).isEmpty();
		assertThat(providers.attribution()).isEqualTo("Streaming data from JustWatch");
	}

	@Test
	void titleNotAvailableInOurRegionHasEmptyProviders() {
		tmdb.stubFor(get(urlPathEqualTo("/tv/2001/watch/providers"))
				.willReturn(okJson("{\"id\": 2001, \"results\": {\"US\": {\"link\": \"x\"}}}")));

		WatchProviders providers = service.watchProviders(MediaType.TV, 2001);

		assertThat(providers.link()).isNull();
		assertThat(providers.stream()).isEmpty();
	}

	@Test
	void parseDateAcceptsDatesAndTimestampsAndRejectsJunk() {
		assertThat(TmdbService.parseDate("2024-02-27")).isEqualTo(LocalDate.of(2024, 2, 27));
		assertThat(TmdbService.parseDate("2024-03-01T00:00:00.000Z")).isEqualTo(LocalDate.of(2024, 3, 1));
		assertThat(TmdbService.parseDate("")).isNull();
		assertThat(TmdbService.parseDate(null)).isNull();
		assertThat(TmdbService.parseDate("not-a-date")).isNull();
	}

	private static void stub(String path, String fixture) {
		tmdb.stubFor(get(urlPathEqualTo(path)).willReturn(okJson(Fixtures.read("tmdb/" + fixture))));
	}
}

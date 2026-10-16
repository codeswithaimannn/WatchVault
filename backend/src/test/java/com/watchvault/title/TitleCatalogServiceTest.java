package com.watchvault.title;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.watchvault.external.ExternalApiUnavailableException;
import com.watchvault.external.tmdb.TmdbService;
import com.watchvault.media.MediaType;

import tools.jackson.databind.json.JsonMapper;

/**
 * The database-cache rules, with TMDB and the repository replaced by Mockito mocks and the
 * clock fixed, so "8 days old" is exact. Refresh after 7 days, never serve after 180.
 */
class TitleCatalogServiceTest {

	private static final Instant NOW = Instant.parse("2026-10-16T12:00:00Z");
	private static final int MOVIE_ID = 1001;
	private static final CacheProperties CACHE = new CacheProperties(Duration.ofDays(7), Duration.ofDays(180));

	private final JsonMapper jsonMapper = JsonMapper.builder().build();
	private TmdbService tmdb;
	private CachedTitleRepository repository;
	private TitleCatalogService catalog;

	@BeforeEach
	void setUp() {
		tmdb = mock(TmdbService.class);
		repository = mock(CachedTitleRepository.class);
		catalog = new TitleCatalogService(tmdb, repository, jsonMapper, CACHE, Clock.fixed(NOW, ZoneOffset.UTC));
	}

	@Test
	void unknownTitleIsFetchedSavedAndStampedWithNow() {
		when(repository.findByMediaTypeAndTmdbId(MediaType.MOVIE, MOVIE_ID)).thenReturn(Optional.empty());
		when(tmdb.movie(MOVIE_ID)).thenReturn(movie("Fresh title", Instant.EPOCH));

		TitleDetails result = catalog.details(MediaType.MOVIE, MOVIE_ID);

		assertThat(result.title()).isEqualTo("Fresh title");
		assertThat(result.fetchedAt()).isEqualTo(NOW);
		verify(repository).save(any(CachedTitle.class));
	}

	@Test
	void recentSavedCopyIsUsedWithoutCallingTmdb() {
		givenSavedCopy("Saved title", NOW.minus(Duration.ofDays(1)));

		TitleDetails result = catalog.details(MediaType.MOVIE, MOVIE_ID);

		assertThat(result.title()).isEqualTo("Saved title");
		verify(tmdb, never()).movie(MOVIE_ID);
	}

	@Test
	void savedCopyOlderThanAWeekIsRefreshed() {
		CachedTitle saved = givenSavedCopy("Old title", NOW.minus(Duration.ofDays(8)));
		when(tmdb.movie(MOVIE_ID)).thenReturn(movie("New title", Instant.EPOCH));

		TitleDetails result = catalog.details(MediaType.MOVIE, MOVIE_ID);

		assertThat(result.title()).isEqualTo("New title");
		assertThat(saved.getFetchedAt()).isEqualTo(NOW); // same row updated, not a second row
		verify(repository).save(saved);
	}

	@Test
	void whenTmdbIsDownAnOlderSavedCopyIsServedWithItsRealDate() {
		Instant eightDaysAgo = NOW.minus(Duration.ofDays(8));
		givenSavedCopy("Old title", eightDaysAgo);
		when(tmdb.movie(MOVIE_ID)).thenThrow(new ExternalApiUnavailableException("TMDB"));

		TitleDetails result = catalog.details(MediaType.MOVIE, MOVIE_ID);

		assertThat(result.title()).isEqualTo("Old title");
		assertThat(result.fetchedAt()).isEqualTo(eightDaysAgo);
	}

	@Test
	void copyPastTheMaximumAgeIsNeverServedEvenIfTmdbIsDown() {
		givenSavedCopy("Expired title", NOW.minus(Duration.ofDays(200)));
		when(tmdb.movie(MOVIE_ID)).thenThrow(new ExternalApiUnavailableException("TMDB"));

		assertThatThrownBy(() -> catalog.details(MediaType.MOVIE, MOVIE_ID))
				.isInstanceOf(ExternalApiUnavailableException.class);
	}

	@Test
	void noSavedCopyAndTmdbDownIsUnavailable() {
		when(repository.findByMediaTypeAndTmdbId(MediaType.MOVIE, MOVIE_ID)).thenReturn(Optional.empty());
		when(tmdb.movie(MOVIE_ID)).thenThrow(new ExternalApiUnavailableException("TMDB"));

		assertThatThrownBy(() -> catalog.details(MediaType.MOVIE, MOVIE_ID))
				.isInstanceOf(ExternalApiUnavailableException.class);
	}

	private CachedTitle givenSavedCopy(String title, Instant fetchedAt) {
		String payload = jsonMapper.writeValueAsString(movie(title, fetchedAt));
		CachedTitle saved = new CachedTitle(MediaType.MOVIE, MOVIE_ID, payload, fetchedAt);
		when(repository.findByMediaTypeAndTmdbId(MediaType.MOVIE, MOVIE_ID)).thenReturn(Optional.of(saved));
		return saved;
	}

	private static TitleDetails movie(String title, Instant fetchedAt) {
		return new TitleDetails(MediaType.MOVIE, MOVIE_ID, title, title, "en", "Overview", null, null, null,
				LocalDate.of(2023, 5, 1), null, 118, "Released", List.of(new Genre(28, "Action")), null, null,
				false, List.of(), fetchedAt);
	}
}

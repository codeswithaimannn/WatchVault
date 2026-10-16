package com.watchvault.title;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;

import com.watchvault.media.MediaType;

/** title_cache against the real MySQL test database; each test is rolled back. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CachedTitleRepositoryTest {

	private static final String PAYLOAD = "{\"title\": \"Example Movie\", \"runtimeMinutes\": 118}";

	private final CachedTitleRepository repository;
	private final TestEntityManager entityManager;

	@Autowired
	CachedTitleRepositoryTest(CachedTitleRepository repository, TestEntityManager entityManager) {
		this.repository = repository;
		this.entityManager = entityManager;
	}

	@Test
	void jsonPayloadAndFetchTimeSurviveAReload() {
		Instant fetchedAt = Instant.now().truncatedTo(ChronoUnit.MICROS); // DATETIME(6) precision
		repository.saveAndFlush(new CachedTitle(MediaType.MOVIE, 1001, PAYLOAD, fetchedAt));
		entityManager.clear();

		CachedTitle reloaded = repository.findByMediaTypeAndTmdbId(MediaType.MOVIE, 1001).orElseThrow();

		assertThat(reloaded.getPayload()).contains("\"title\"").contains("Example Movie");
		assertThat(reloaded.getFetchedAt()).isEqualTo(fetchedAt);
	}

	@Test
	void payloadIsStoredAsARealJsonObjectNotAQuotedString() {
		repository.saveAndFlush(new CachedTitle(MediaType.MOVIE, 1001, PAYLOAD, Instant.now()));

		Object jsonType = entityManager.getEntityManager()
				.createNativeQuery("SELECT JSON_TYPE(payload) FROM title_cache WHERE tmdb_id = 1001")
				.getSingleResult();

		assertThat(jsonType).isEqualTo("OBJECT");
	}

	@Test
	void sameIdIsAllowedOnceForMovieAndOnceForTv() {
		repository.saveAndFlush(new CachedTitle(MediaType.MOVIE, 1001, PAYLOAD, Instant.now()));
		repository.saveAndFlush(new CachedTitle(MediaType.TV, 1001, PAYLOAD, Instant.now()));

		assertThatThrownBy(() -> repository.saveAndFlush(new CachedTitle(MediaType.MOVIE, 1001, PAYLOAD, Instant.now())))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void purgeDeletesOnlyCopiesFetchedBeforeTheCutoff() {
		Instant now = Instant.now();
		repository.saveAndFlush(new CachedTitle(MediaType.MOVIE, 1, PAYLOAD, now.minus(Duration.ofDays(200))));
		repository.saveAndFlush(new CachedTitle(MediaType.MOVIE, 2, PAYLOAD, now.minus(Duration.ofDays(10))));

		int deleted = repository.deleteFetchedBefore(now.minus(Duration.ofDays(180)));
		entityManager.clear();

		assertThat(deleted).isEqualTo(1);
		assertThat(repository.findByMediaTypeAndTmdbId(MediaType.MOVIE, 1)).isEmpty();
		assertThat(repository.findByMediaTypeAndTmdbId(MediaType.MOVIE, 2)).isPresent();
	}
}

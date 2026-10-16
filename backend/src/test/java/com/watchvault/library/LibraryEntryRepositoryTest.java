package com.watchvault.library;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;

import com.watchvault.media.MediaType;
import com.watchvault.user.User;
import com.watchvault.user.UserRepository;

/** library_entries against the real MySQL test database; each test is rolled back. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class LibraryEntryRepositoryTest {

	private static final LocalDate TODAY = LocalDate.of(2026, 10, 16);

	private final LibraryEntryRepository entries;
	private final UserRepository users;
	private final TestEntityManager entityManager;
	private Long aliceId;

	@Autowired
	LibraryEntryRepositoryTest(LibraryEntryRepository entries, UserRepository users, TestEntityManager entityManager) {
		this.entries = entries;
		this.users = users;
		this.entityManager = entityManager;
	}

	@BeforeEach
	void createUser() {
		aliceId = users.save(new User("alice@example.com", "Alice", "hash")).getId();
	}

	@Test
	void entrySurvivesAReloadIncludingRatingAndCompletionDate() {
		LibraryEntry entry = new LibraryEntry(aliceId, MediaType.MOVIE, 1001);
		entry.update(LibraryStatus.COMPLETED, 8, "Loved the score.", TODAY);
		entries.saveAndFlush(entry);
		entityManager.clear();

		LibraryEntry reloaded = entries.findByUserIdAndMediaTypeAndTmdbId(aliceId, MediaType.MOVIE, 1001).orElseThrow();

		assertThat(reloaded.getStatus()).isEqualTo(LibraryStatus.COMPLETED);
		assertThat(reloaded.getRating()).isEqualTo(8);
		assertThat(reloaded.getNotes()).isEqualTo("Loved the score.");
		assertThat(reloaded.getCompletedOn()).isEqualTo(TODAY);
		assertThat(reloaded.getCreatedAt()).isNotNull();
	}

	@Test
	void aTitleCanBeInTheLibraryOnlyOnce() {
		entries.saveAndFlush(planned(aliceId, MediaType.MOVIE, 1001));

		assertThatThrownBy(() -> entries.saveAndFlush(planned(aliceId, MediaType.MOVIE, 1001)))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void sameIdAsMovieAndAsShowAreDifferentEntries() {
		entries.saveAndFlush(planned(aliceId, MediaType.MOVIE, 1001));
		entries.saveAndFlush(planned(aliceId, MediaType.TV, 1001));

		assertThat(entries.findByUserIdOrderByUpdatedAtDesc(aliceId)).hasSize(2);
	}

	@Test
	void ratingOutsideOneToTenIsRejectedByTheDatabase() {
		LibraryEntry entry = new LibraryEntry(aliceId, MediaType.MOVIE, 1001);
		entry.update(LibraryStatus.COMPLETED, 11, null, TODAY);

		assertThatThrownBy(() -> entries.saveAndFlush(entry)).isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void deletingTheUserDeletesTheirLibrary() {
		entries.saveAndFlush(planned(aliceId, MediaType.MOVIE, 1001));
		entityManager.clear();

		users.deleteById(aliceId);
		users.flush();
		entityManager.clear();

		assertThat(entries.findByUserIdOrderByUpdatedAtDesc(aliceId)).isEmpty();
	}

	private static LibraryEntry planned(Long userId, MediaType mediaType, int tmdbId) {
		LibraryEntry entry = new LibraryEntry(userId, mediaType, tmdbId);
		entry.update(LibraryStatus.PLAN_TO_WATCH, null, null, TODAY);
		return entry;
	}
}

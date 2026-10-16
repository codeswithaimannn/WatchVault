package com.watchvault.user;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import com.watchvault.media.MediaType;

/** Real MySQL test database; each test is rolled back. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserPreferencesRepositoryTest {

	private static final int SCI_FI_GENRE_ID = 878;

	private final UserRepository users;
	private final UserPreferencesRepository preferences;
	private final TestEntityManager entityManager;

	@Autowired
	UserPreferencesRepositoryTest(UserRepository users, UserPreferencesRepository preferences,
			TestEntityManager entityManager) {
		this.users = users;
		this.preferences = preferences;
		this.entityManager = entityManager;
	}

	@Test
	void newPreferencesDefaultToGbAndShareTheUserId() {
		User user = users.save(new User("alice@example.com", "hash"));

		UserPreferences saved = preferences.saveAndFlush(new UserPreferences(user));

		assertThat(saved.getUserId()).isEqualTo(user.getId());
		assertThat(saved.getRegion()).isEqualTo("GB");
	}

	@Test
	void languagesAndGenresSurviveAReloadFromTheDatabase() {
		UserPreferences prefs = new UserPreferences(users.save(new User("bob@example.com", "hash")));
		prefs.replaceLanguages(Set.of("en", "JA"));
		prefs.replaceGenres(Set.of(new PreferredGenre(MediaType.MOVIE, SCI_FI_GENRE_ID)));
		Long userId = preferences.saveAndFlush(prefs).getUserId();
		entityManager.clear(); // forget cached objects so the next read really hits MySQL

		UserPreferences reloaded = preferences.findById(userId).orElseThrow();

		assertThat(reloaded.getPreferredLanguages()).containsExactlyInAnyOrder("en", "ja");
		assertThat(reloaded.getPreferredGenres())
				.containsExactly(new PreferredGenre(MediaType.MOVIE, SCI_FI_GENRE_ID));
	}

	@Test
	void sameGenreIdCanBeStoredForMovieAndTv() {
		UserPreferences prefs = new UserPreferences(users.save(new User("carol@example.com", "hash")));
		prefs.replaceGenres(Set.of(
				new PreferredGenre(MediaType.MOVIE, SCI_FI_GENRE_ID),
				new PreferredGenre(MediaType.TV, SCI_FI_GENRE_ID)));
		Long userId = preferences.saveAndFlush(prefs).getUserId();
		entityManager.clear();

		assertThat(preferences.findById(userId).orElseThrow().getPreferredGenres()).hasSize(2);
	}

	@Test
	void deletingAUserDeletesTheirPreferences() {
		User user = users.save(new User("dave@example.com", "hash"));
		UserPreferences prefs = new UserPreferences(user);
		prefs.replaceLanguages(Set.of("en"));
		preferences.saveAndFlush(prefs);
		entityManager.clear();

		users.deleteById(user.getId());
		users.flush();
		entityManager.clear();

		// ON DELETE CASCADE in V1 removed the preferences and language rows.
		assertThat(preferences.findById(user.getId())).isEmpty();
	}
}

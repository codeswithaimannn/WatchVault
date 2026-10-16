package com.watchvault.library;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.watchvault.media.MediaType;

/** The completedOn rules, as a plain unit test. */
class LibraryEntryTest {

	private static final LocalDate MONDAY = LocalDate.of(2026, 10, 12);
	private static final LocalDate FRIDAY = LocalDate.of(2026, 10, 16);

	private LibraryEntry entry;

	@BeforeEach
	void setUp() {
		entry = new LibraryEntry(1L, MediaType.MOVIE, 1001);
	}

	@Test
	void completingSetsTheCompletionDate() {
		entry.update(LibraryStatus.WATCHING, null, null, MONDAY);
		entry.update(LibraryStatus.COMPLETED, 8, null, FRIDAY);

		assertThat(entry.getCompletedOn()).isEqualTo(FRIDAY);
	}

	@Test
	void editingACompletedEntryKeepsTheOriginalDate() {
		entry.update(LibraryStatus.COMPLETED, null, null, MONDAY);
		entry.update(LibraryStatus.COMPLETED, 9, "Rewatch soon", FRIDAY); // only rating/notes changed

		assertThat(entry.getCompletedOn()).isEqualTo(MONDAY);
		assertThat(entry.getRating()).isEqualTo(9);
	}

	@Test
	void movingAwayFromCompletedClearsTheDate() {
		entry.update(LibraryStatus.COMPLETED, null, null, MONDAY);
		entry.update(LibraryStatus.WATCHING, null, null, FRIDAY);

		assertThat(entry.getCompletedOn()).isNull();
	}

	@Test
	void otherStatusesHaveNoCompletionDate() {
		entry.update(LibraryStatus.PLAN_TO_WATCH, null, null, MONDAY);

		assertThat(entry.getCompletedOn()).isNull();
	}
}

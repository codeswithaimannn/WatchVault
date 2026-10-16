package com.watchvault.library;

import java.time.Instant;
import java.time.LocalDate;

import com.watchvault.media.MediaType;
import com.watchvault.title.TitleDetails;

/**
 * One library entry as the API shows it: the user's own data, plus title info from the TMDB cache.
 *
 * @param title null when the details can't be fetched right now (TMDB down and nothing saved).
 *              The user's status, rating and notes are always present regardless.
 */
public record LibraryEntryResponse(
		MediaType mediaType,
		int tmdbId,
		LibraryStatus status,
		Integer rating,
		String notes,
		LocalDate completedOn,
		Instant addedAt,
		Instant updatedAt,
		LibraryTitle title) {

	static LibraryEntryResponse from(LibraryEntry entry, TitleDetails details) {
		return new LibraryEntryResponse(entry.getMediaType(), entry.getTmdbId(), entry.getStatus(), entry.getRating(),
				entry.getNotes(), entry.getCompletedOn(), entry.getCreatedAt(), entry.getUpdatedAt(),
				details == null ? null : LibraryTitle.from(details));
	}

	/** The few title fields a library card needs. */
	public record LibraryTitle(String title, String posterPath, LocalDate releaseDate, boolean anime) {

		static LibraryTitle from(TitleDetails details) {
			return new LibraryTitle(details.title(), details.posterPath(), details.releaseDate(), details.anime());
		}
	}
}

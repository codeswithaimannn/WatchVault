package com.watchvault.library;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.watchvault.media.MediaType;
import com.watchvault.title.TitleCatalogService;
import com.watchvault.title.TitleDetails;
import com.watchvault.title.TitleKey;

/**
 * The logged-in user's library. Every method takes the owner's id (from the token, never from
 * the request), and every query filters on it.
 *
 * Not @Transactional: the title lookups may call TMDB, and a database transaction should never
 * stay open while we wait on another server. Each repository call runs in its own transaction.
 */
@Service
public class LibraryService {

	/** "Completed today" means today in the UK, not in UTC (they differ around midnight in summer). */
	private static final ZoneId UK = ZoneId.of("Europe/London");

	private final LibraryEntryRepository entries;
	private final TitleCatalogService catalog;
	private final Clock clock;

	public LibraryService(LibraryEntryRepository entries, TitleCatalogService catalog, Clock clock) {
		this.entries = entries;
		this.catalog = catalog;
		this.clock = clock;
	}

	/** The whole library, most recently changed first. The frontend filters and counts. */
	public List<LibraryEntryResponse> list(Long userId) {
		List<LibraryEntry> library = entries.findByUserIdOrderByUpdatedAtDesc(userId);
		Map<TitleKey, TitleDetails> titles = catalog.detailsForList(library.stream().map(LibraryService::keyOf).toList());
		return library.stream()
				.map(entry -> LibraryEntryResponse.from(entry, titles.get(keyOf(entry))))
				.toList();
	}

	public LibraryEntryResponse get(Long userId, MediaType mediaType, int tmdbId) {
		LibraryEntry entry = entries.findByUserIdAndMediaTypeAndTmdbId(userId, mediaType, tmdbId)
				.orElseThrow(LibraryEntryNotFoundException::new);
		return withTitle(entry);
	}

	/**
	 * Adds the title, or updates it if it's already there. A new title is checked against TMDB
	 * first, so the library can't fill up with made-up ids (unknown id: 404; TMDB down: 503).
	 */
	public LibraryEntryResponse save(Long userId, MediaType mediaType, int tmdbId, SaveLibraryEntryRequest request) {
		LibraryEntry entry = entries.findByUserIdAndMediaTypeAndTmdbId(userId, mediaType, tmdbId)
				.orElseGet(() -> {
					catalog.details(mediaType, tmdbId);
					return new LibraryEntry(userId, mediaType, tmdbId);
				});
		entry.update(request.status(), request.rating(), request.notes(), LocalDate.now(clock.withZone(UK)));
		try {
			return withTitle(entries.saveAndFlush(entry));
		} catch (DataIntegrityViolationException e) {
			// Added twice at the same moment (e.g. a double click): update the entry that won.
			LibraryEntry existing = entries.findByUserIdAndMediaTypeAndTmdbId(userId, mediaType, tmdbId).orElseThrow(() -> e);
			existing.update(request.status(), request.rating(), request.notes(), LocalDate.now(clock.withZone(UK)));
			return withTitle(entries.saveAndFlush(existing));
		}
	}

	/** Removing something that isn't there is not an error: the result is the same. */
	public void remove(Long userId, MediaType mediaType, int tmdbId) {
		entries.findByUserIdAndMediaTypeAndTmdbId(userId, mediaType, tmdbId).ifPresent(entries::delete);
	}

	private LibraryEntryResponse withTitle(LibraryEntry entry) {
		TitleDetails details = catalog.detailsForList(List.of(keyOf(entry))).get(keyOf(entry));
		return LibraryEntryResponse.from(entry, details);
	}

	private static TitleKey keyOf(LibraryEntry entry) {
		return new TitleKey(entry.getMediaType(), entry.getTmdbId());
	}
}

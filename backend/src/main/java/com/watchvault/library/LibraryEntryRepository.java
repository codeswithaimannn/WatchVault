package com.watchvault.library;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.watchvault.media.MediaType;

/** Every method takes the owner's userId: there is deliberately no "find by entry id" for the API. */
public interface LibraryEntryRepository extends JpaRepository<LibraryEntry, Long> {

	List<LibraryEntry> findByUserIdOrderByUpdatedAtDesc(Long userId);

	Optional<LibraryEntry> findByUserIdAndMediaTypeAndTmdbId(Long userId, MediaType mediaType, int tmdbId);
}

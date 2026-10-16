package com.watchvault.title;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import com.watchvault.media.MediaType;

public interface CachedTitleRepository extends JpaRepository<CachedTitle, Long> {

	Optional<CachedTitle> findByMediaTypeAndTmdbId(MediaType mediaType, int tmdbId);

	/** One DELETE statement (not one per row). Returns how many rows were removed. */
	@Modifying
	@Transactional
	@Query("DELETE FROM CachedTitle c WHERE c.fetchedAt < :cutoff")
	int deleteFetchedBefore(Instant cutoff);
}

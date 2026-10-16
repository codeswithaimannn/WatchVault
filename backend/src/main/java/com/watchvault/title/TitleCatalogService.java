package com.watchvault.title;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.watchvault.external.ExternalApiUnavailableException;
import com.watchvault.external.ExternalResourceNotFoundException;
import com.watchvault.external.tmdb.TmdbService;
import com.watchvault.media.MediaType;

import tools.jackson.databind.json.JsonMapper;

/**
 * The app's single entry point for film/TV metadata. Controllers call this, never TMDB directly.
 * Adds caching on top of TmdbService: Caffeine (@Cacheable) for short-lived data, the database
 * for title details.
 */
@Service
public class TitleCatalogService {

	private static final Logger log = LoggerFactory.getLogger(TitleCatalogService.class);

	/** Caps TMDB calls when building one list, so a large library can't make a page hang. */
	static final int MAX_FETCHES_PER_LIST = 20;

	private final TmdbService tmdb;
	private final CachedTitleRepository cachedTitles;
	private final JsonMapper jsonMapper;
	private final CacheProperties cacheProperties;
	private final Clock clock;

	public TitleCatalogService(TmdbService tmdb, CachedTitleRepository cachedTitles, JsonMapper jsonMapper,
			CacheProperties cacheProperties, Clock clock) {
		this.tmdb = tmdb;
		this.cachedTitles = cachedTitles;
		this.jsonMapper = jsonMapper;
		this.cacheProperties = cacheProperties;
		this.clock = clock;
	}

	@Cacheable(cacheNames = CacheConfig.GENRES, key = "#mediaType")
	public List<Genre> genres(MediaType mediaType) {
		return tmdb.genres(mediaType);
	}

	/** Expects a query already trimmed by the caller; case doesn't matter for the cache key. */
	@Cacheable(cacheNames = CacheConfig.SEARCH, key = "#query.toLowerCase() + '|' + #page")
	public SearchResults search(String query, int page) {
		return tmdb.search(query, page);
	}

	@Cacheable(cacheNames = CacheConfig.SEASONS, key = "#tvTmdbId + '|' + #seasonNumber")
	public SeasonDetails season(int tvTmdbId, int seasonNumber) {
		return tmdb.season(tvTmdbId, seasonNumber);
	}

	@Cacheable(cacheNames = CacheConfig.PROVIDERS, key = "#mediaType + '|' + #tmdbId")
	public WatchProviders watchProviders(MediaType mediaType, int tmdbId) {
		return tmdb.watchProviders(mediaType, tmdbId);
	}

	/**
	 * Saved copy if it is recent; otherwise fetch from TMDB and save. If TMDB is down, an older
	 * saved copy is better than nothing: it is returned with its original fetchedAt, so the UI
	 * can say how old it is. Copies past the maximum age are never served.
	 */
	public TitleDetails details(MediaType mediaType, int tmdbId) {
		Instant now = clock.instant();
		Optional<CachedTitle> saved = cachedTitles.findByMediaTypeAndTmdbId(mediaType, tmdbId)
				.filter(copy -> !isOlderThan(copy, cacheProperties.titleMaxAge(), now));

		if (saved.isPresent() && !isOlderThan(saved.get(), cacheProperties.titleRefreshAfter(), now)) {
			return fromJson(saved.get());
		}
		try {
			TitleDetails fresh = fetch(mediaType, tmdbId).withFetchedAt(now);
			save(saved, fresh);
			return fresh;
		} catch (ExternalApiUnavailableException e) {
			if (saved.isPresent()) {
				log.info("TMDB unavailable; serving saved {} {} from {}", mediaType, tmdbId, saved.get().getFetchedAt());
				return fromJson(saved.get());
			}
			throw e;
		}
	}

	/**
	 * Details for a whole list (e.g. a library): one database query for the saved copies, which are
	 * used even if more than a week old (lists must be fast and work while TMDB is down).
	 * Missing titles are fetched, at most {@value #MAX_FETCHES_PER_LIST} per call, and fetching stops
	 * at the first sign TMDB is down. Titles we couldn't get are simply absent from the result.
	 */
	public Map<TitleKey, TitleDetails> detailsForList(Collection<TitleKey> keys) {
		Set<TitleKey> wanted = Set.copyOf(keys);
		Map<TitleKey, TitleDetails> found = new HashMap<>();
		if (wanted.isEmpty()) {
			return found;
		}
		Instant now = clock.instant();
		Set<Integer> tmdbIds = wanted.stream().map(TitleKey::tmdbId).collect(Collectors.toSet());
		for (CachedTitle copy : cachedTitles.findByTmdbIdIn(tmdbIds)) {
			TitleKey key = new TitleKey(copy.getMediaType(), copy.getTmdbId());
			if (wanted.contains(key) && !isOlderThan(copy, cacheProperties.titleMaxAge(), now)) {
				found.put(key, fromJson(copy));
			}
		}
		fetchMissing(wanted, found);
		return found;
	}

	private void fetchMissing(Set<TitleKey> wanted, Map<TitleKey, TitleDetails> found) {
		int fetched = 0;
		for (TitleKey key : wanted) {
			if (found.containsKey(key)) {
				continue;
			}
			if (fetched == MAX_FETCHES_PER_LIST) {
				return; // the rest are filled in on later page loads
			}
			fetched++;
			try {
				found.put(key, details(key.mediaType(), key.tmdbId()));
			} catch (ExternalResourceNotFoundException e) {
				log.info("Title {} no longer exists on TMDB", key);
			} catch (ExternalApiUnavailableException e) {
				return; // TMDB is down: don't make the user wait for every remaining title
			}
		}
	}

	private TitleDetails fetch(MediaType mediaType, int tmdbId) {
		return mediaType == MediaType.MOVIE ? tmdb.movie(tmdbId) : tmdb.tv(tmdbId);
	}

	private void save(Optional<CachedTitle> existing, TitleDetails details) {
		String payload = jsonMapper.writeValueAsString(details);
		CachedTitle row = existing
				.map(copy -> {
					copy.refresh(payload, details.fetchedAt());
					return copy;
				})
				.orElseGet(() -> new CachedTitle(details.mediaType(), details.tmdbId(), payload, details.fetchedAt()));
		try {
			cachedTitles.save(row);
		} catch (DataIntegrityViolationException e) {
			// Another request saved the same title at the same moment. Its copy is just as good.
			log.debug("Title {} {} was cached concurrently", details.mediaType(), details.tmdbId());
		}
	}

	private TitleDetails fromJson(CachedTitle copy) {
		return jsonMapper.readValue(copy.getPayload(), TitleDetails.class);
	}

	private static boolean isOlderThan(CachedTitle copy, Duration age, Instant now) {
		return copy.getFetchedAt().isBefore(now.minus(age));
	}
}

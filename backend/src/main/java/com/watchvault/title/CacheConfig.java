package com.watchvault.title;

import java.time.Clock;
import java.time.Duration;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.watchvault.external.tvmaze.TvmazeService;

/**
 * Short-lived in-memory caches (Caffeine) for TMDB and TVmaze data that is cheap to fetch again.
 * Lifetimes sit inside TMDB's own Cache-Control hints (roughly 1-8 hours, DECISIONS.md).
 * Title details are cached longer, in the database: see TitleCatalogService.
 */
@Configuration
@EnableCaching
@EnableScheduling
public class CacheConfig {

	static final String SEARCH = "tmdbSearch";
	static final String GENRES = "tmdbGenres";
	static final String PROVIDERS = "tmdbProviders";
	static final String SEASONS = "tmdbSeasons";

	private static final Duration SEARCH_TTL = Duration.ofHours(1);
	private static final Duration GENRES_TTL = Duration.ofHours(24);
	private static final Duration PROVIDERS_TTL = Duration.ofHours(6); // availability changes often
	private static final Duration SEASONS_TTL = Duration.ofHours(6); // new episodes get added
	private static final Duration TVMAZE_TTL = Duration.ofHours(1); // TVmaze itself caches for 60 minutes
	private static final Duration TVMAZE_LOOKUP_TTL = Duration.ofHours(24); // show ids rarely change

	/** Caps memory use: the least recently used entries are dropped first. */
	private static final int MAX_ENTRIES = 1_000;

	@Bean
	CacheManager cacheManager() {
		CaffeineCacheManager manager = new CaffeineCacheManager();
		manager.registerCustomCache(SEARCH, build(SEARCH_TTL));
		manager.registerCustomCache(GENRES, build(GENRES_TTL));
		manager.registerCustomCache(PROVIDERS, build(PROVIDERS_TTL));
		manager.registerCustomCache(SEASONS, build(SEASONS_TTL));
		manager.registerCustomCache(TvmazeService.LOOKUP_CACHE, build(TVMAZE_LOOKUP_TTL));
		manager.registerCustomCache(TvmazeService.EPISODES_CACHE, build(TVMAZE_TTL));
		manager.registerCustomCache(TvmazeService.SCHEDULE_CACHE, build(TVMAZE_TTL));
		manager.setAllowNullValues(false);
		return manager;
	}

	/** Injected wherever "now" matters, so tests can control time. */
	@Bean
	Clock clock() {
		return Clock.systemUTC();
	}

	private static com.github.benmanes.caffeine.cache.Cache<Object, Object> build(Duration timeToLive) {
		return Caffeine.newBuilder()
				.expireAfterWrite(timeToLive)
				.maximumSize(MAX_ENTRIES)
				.recordStats() // hit/miss counts, exposed as metrics
				.build();
	}
}

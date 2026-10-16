package com.watchvault.title;

import java.time.Clock;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Deletes saved TMDB titles past the maximum age, once a day (TMDB terms: no caching beyond
 * 6 months). TitleCatalogService also refuses to serve such copies, in case this has not run yet.
 */
@Component
public class TitleCachePurgeJob {

	private static final Logger log = LoggerFactory.getLogger(TitleCachePurgeJob.class);

	private final CachedTitleRepository cachedTitles;
	private final CacheProperties cacheProperties;
	private final Clock clock;

	public TitleCachePurgeJob(CachedTitleRepository cachedTitles, CacheProperties cacheProperties, Clock clock) {
		this.cachedTitles = cachedTitles;
		this.cacheProperties = cacheProperties;
		this.clock = clock;
	}

	/** 03:30 every night (server time), when the app is quiet. */
	@Scheduled(cron = "0 30 3 * * *")
	public int purgeExpired() {
		Instant cutoff = clock.instant().minus(cacheProperties.titleMaxAge());
		int deleted = cachedTitles.deleteFetchedBefore(cutoff);
		log.info("Title cache purge: deleted {} entries fetched before {}", deleted, cutoff);
		return deleted;
	}
}

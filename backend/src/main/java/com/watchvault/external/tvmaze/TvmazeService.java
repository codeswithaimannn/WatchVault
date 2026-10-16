package com.watchvault.external.tvmaze;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.watchvault.external.ExternalResourceNotFoundException;
import com.watchvault.external.tvmaze.TvmazeResponses.Episode;
import com.watchvault.external.tvmaze.TvmazeResponses.Show;
import com.watchvault.schedule.DaySchedule;
import com.watchvault.schedule.EpisodeAiring;
import com.watchvault.schedule.EpisodeAiring.Source;
import com.watchvault.schedule.TvShowLink;

/**
 * Episode dates and UK schedules from TVmaze, translated into our DTOs.
 * Cached for an hour: TVmaze caches its own responses for 60 minutes, so asking more often gains nothing.
 */
@Service
public class TvmazeService {

	public static final String LOOKUP_CACHE = "tvmazeLookup";
	public static final String EPISODES_CACHE = "tvmazeEpisodes";
	public static final String SCHEDULE_CACHE = "tvmazeSchedule";

	private final TvmazeClient client;

	public TvmazeService(TvmazeClient client) {
		this.client = client;
	}

	/** TVmaze answers with a 301 redirect to the show; our HTTP client follows it. Unknown id = empty. */
	@Cacheable(cacheNames = LOOKUP_CACHE, key = "'imdb:' + #imdbId", unless = "#result == null")
	public Optional<TvShowLink> lookupByImdb(String imdbId) {
		return lookup(Map.of("imdb", imdbId));
	}

	/** Fallback when TMDB has no IMDb id for a show. */
	@Cacheable(cacheNames = LOOKUP_CACHE, key = "'tvdb:' + #tvdbId", unless = "#result == null")
	public Optional<TvShowLink> lookupByTvdb(int tvdbId) {
		return lookup(Map.of("thetvdb", tvdbId));
	}

	@Cacheable(cacheNames = EPISODES_CACHE, key = "#tvmazeShowId")
	public List<EpisodeAiring> episodes(int tvmazeShowId) {
		Show show = client.get("/shows/" + tvmazeShowId, Map.of(), Show.class);
		Episode[] episodes = client.get("/shows/" + tvmazeShowId + "/episodes", Map.of(), Episode[].class);
		return Arrays.stream(episodes)
				.map(episode -> toAiring(episode, show, sourceOf(show)))
				.toList();
	}

	/**
	 * Broadcast TV and streaming releases for one day, in one list. TVmaze keeps them in two
	 * separate schedules. An episode in both (e.g. on BBC One and iPlayer) is listed once, as broadcast.
	 */
	@Cacheable(cacheNames = SCHEDULE_CACHE, key = "#date")
	public DaySchedule schedule(LocalDate date) {
		Map<String, ?> params = Map.of("country", client.country(), "date", date.toString());
		Episode[] broadcast = client.get("/schedule", params, Episode[].class);
		Episode[] streaming = client.get("/schedule/web", params, Episode[].class);

		Map<Integer, EpisodeAiring> byEpisodeId = new LinkedHashMap<>();
		addAll(byEpisodeId, broadcast, Source.BROADCAST);
		addAll(byEpisodeId, streaming, Source.STREAMING);
		return new DaySchedule(date, client.country(), List.copyOf(byEpisodeId.values()), DaySchedule.TVMAZE_ATTRIBUTION);
	}

	private Optional<TvShowLink> lookup(Map<String, ?> params) {
		try {
			Show show = client.get("/lookup/shows", params, Show.class);
			return Optional.of(toLink(show));
		} catch (ExternalResourceNotFoundException e) {
			return Optional.empty(); // TVmaze doesn't know this show: not an error
		}
	}

	private static void addAll(Map<Integer, EpisodeAiring> byEpisodeId, Episode[] episodes, Source source) {
		for (Episode episode : episodes) {
			Show show = episode.showOrEmbedded();
			if (show != null) {
				byEpisodeId.putIfAbsent(episode.id(), toAiring(episode, show, source));
			}
		}
	}

	private static EpisodeAiring toAiring(Episode episode, Show show, Source source) {
		LocalTime airTime = source == Source.STREAMING ? null : parseTime(episode.airtime());
		return new EpisodeAiring(show.id(), show.name(), imdbId(show), episode.season(), episode.number(),
				episode.name(), parseDate(episode.airdate()), airTime, episode.runtime(), channel(show), source);
	}

	private static TvShowLink toLink(Show show) {
		Integer tvdbId = show.externals() == null ? null : show.externals().thetvdb();
		return new TvShowLink(show.id(), show.name(), imdbId(show), tvdbId, channel(show), show.status());
	}

	/** A show with a network is broadcast TV; otherwise it's a streaming (web) show. */
	private static Source sourceOf(Show show) {
		return show.network() != null ? Source.BROADCAST : Source.STREAMING;
	}

	private static String channel(Show show) {
		if (show.network() != null) {
			return show.network().name();
		}
		return show.webChannel() == null ? null : show.webChannel().name();
	}

	private static String imdbId(Show show) {
		return show.externals() == null ? null : show.externals().imdb();
	}

	private static LocalDate parseDate(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		try {
			return LocalDate.parse(value);
		} catch (DateTimeParseException e) {
			return null;
		}
	}

	/** "" means unknown: no time at all rather than a made-up one. */
	private static LocalTime parseTime(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		try {
			return LocalTime.parse(value);
		} catch (DateTimeParseException e) {
			return null;
		}
	}
}

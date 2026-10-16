package com.watchvault.schedule;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * When an episode airs, from TVmaze.
 *
 * @param season         null for some specials
 * @param number         episode number in the season; null for some specials
 * @param airDate        null if not announced
 * @param airTime        UK local time for BROADCAST only. Always null for STREAMING, because
 *                       TVmaze's streaming times are placeholders; we show the date only.
 * @param runtimeMinutes TVmaze's estimate, or null
 */
public record EpisodeAiring(
		int tvmazeShowId,
		String showName,
		String imdbId,
		Integer season,
		Integer number,
		String episodeName,
		LocalDate airDate,
		LocalTime airTime,
		Integer runtimeMinutes,
		String channel,
		Source source) {

	public enum Source {
		BROADCAST,
		STREAMING
	}
}

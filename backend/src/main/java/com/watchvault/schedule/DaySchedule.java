package com.watchvault.schedule;

import java.time.LocalDate;
import java.util.List;

/**
 * Everything airing in one country on one day: broadcast TV and streaming releases together.
 * TVmaze data is CC BY-SA 4.0, so the credit travels with the data and must be shown.
 */
public record DaySchedule(LocalDate date, String country, List<EpisodeAiring> episodes, String attribution) {

	public static final String TVMAZE_ATTRIBUTION = "Schedule data from TVmaze (CC BY-SA 4.0)";
}

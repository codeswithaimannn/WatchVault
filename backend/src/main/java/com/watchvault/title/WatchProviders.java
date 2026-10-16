package com.watchvault.title;

import java.util.List;

/**
 * Where a title can be watched in one country. Data comes from JustWatch via TMDB:
 * wherever this is shown, the JustWatch credit is mandatory (TMDB terms).
 *
 * @param link TMDB's watch page for this title (we get no direct deep links), or null
 */
public record WatchProviders(
		String region,
		String link,
		List<Provider> stream,
		List<Provider> rent,
		List<Provider> buy,
		String attribution) {

	public static final String JUSTWATCH_ATTRIBUTION = "Streaming data from JustWatch";

	public record Provider(int id, String name, String logoPath) {
	}
}

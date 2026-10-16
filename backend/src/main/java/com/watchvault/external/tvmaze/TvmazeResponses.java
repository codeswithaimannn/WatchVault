package com.watchvault.external.tvmaze;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** TVmaze's JSON shapes, only the fields we use. Package-private, like TmdbResponses. */
final class TvmazeResponses {

	private TvmazeResponses() {
	}

	/** network is set for broadcast TV, webChannel for streaming; usually only one of them. */
	@JsonIgnoreProperties(ignoreUnknown = true)
	record Show(int id, String name, String status, Externals externals, Channel network, Channel webChannel) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record Externals(String imdb, Integer thetvdb) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record Channel(String name) {
	}

	/**
	 * airtime is "HH:mm" UK local time, or "" when unknown. The broadcast schedule puts the
	 * show in {@code show}; the streaming schedule puts it in {@code _embedded.show}.
	 */
	@JsonIgnoreProperties(ignoreUnknown = true)
	record Episode(
			int id,
			Integer season,
			Integer number,
			String name,
			String airdate,
			String airtime,
			Integer runtime,
			Show show,
			@JsonProperty("_embedded") Embedded embedded) {

		Show showOrEmbedded() {
			if (show != null) {
				return show;
			}
			return embedded == null ? null : embedded.show();
		}
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record Embedded(Show show) {
	}
}

package com.watchvault.schedule;

/**
 * A TVmaze show matched to one of our TMDB shows (by IMDb id, or TVDB id as a fallback).
 *
 * @param channel broadcast network or streaming service name, or null if TVmaze has neither
 */
public record TvShowLink(int tvmazeId, String name, String imdbId, Integer tvdbId, String channel, String status) {
}

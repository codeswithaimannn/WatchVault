package com.watchvault.title;

/** A TMDB genre. Ids differ between movies and TV, so always use it with a media type. */
public record Genre(int id, String name) {
}

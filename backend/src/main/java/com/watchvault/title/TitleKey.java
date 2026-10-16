package com.watchvault.title;

import com.watchvault.media.MediaType;

/** Identifies a title: TMDB ids are only unique within a media type. */
public record TitleKey(MediaType mediaType, int tmdbId) {
}

package com.watchvault.media;

import com.watchvault.common.InvalidInputException;

/** Converts the media type in a URL ("movie" or "tv") into our enum. */
public final class MediaTypePaths {

	private MediaTypePaths() {
	}

	public static MediaType fromPath(String value) {
		return switch (value) {
			case "movie" -> MediaType.MOVIE;
			case "tv" -> MediaType.TV;
			default -> throw new InvalidInputException("Media type must be 'movie' or 'tv'.");
		};
	}
}

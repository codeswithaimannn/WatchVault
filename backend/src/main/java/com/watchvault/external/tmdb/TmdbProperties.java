package com.watchvault.external.tmdb;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Settings under watchvault.tmdb.
 *
 * @param baseUrl   API root, e.g. https://api.themoviedb.org/3 (tests point it at WireMock)
 * @param readToken TMDB "API Read Access Token", from env var TMDB_READ_TOKEN only
 * @param language  language for titles and overviews, e.g. en-GB
 * @param region    country for release dates and watch providers, e.g. GB
 */
@Validated
@ConfigurationProperties("watchvault.tmdb")
public record TmdbProperties(
		@NotBlank String baseUrl,
		@NotBlank String readToken,
		@NotBlank String language,
		@NotBlank @Pattern(regexp = "[A-Z]{2}") String region) {
}

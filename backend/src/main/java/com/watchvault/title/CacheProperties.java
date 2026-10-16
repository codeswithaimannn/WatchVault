package com.watchvault.title;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotNull;

/**
 * Settings under watchvault.cache.
 *
 * @param titleRefreshAfter fetch a fresh copy when the saved one is older than this
 * @param titleMaxAge       never keep or serve a copy older than this (TMDB terms: under 6 months)
 */
@Validated
@ConfigurationProperties("watchvault.cache")
public record CacheProperties(@NotNull Duration titleRefreshAfter, @NotNull Duration titleMaxAge) {
}

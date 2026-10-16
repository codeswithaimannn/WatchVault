package com.watchvault.external.tvmaze;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Settings under watchvault.tvmaze. TVmaze needs no API key.
 *
 * @param baseUrl API root, e.g. https://api.tvmaze.com (tests point it at WireMock)
 * @param country country for schedules, e.g. GB
 */
@Validated
@ConfigurationProperties("watchvault.tvmaze")
public record TvmazeProperties(@NotBlank String baseUrl, @NotBlank @Pattern(regexp = "[A-Z]{2}") String country) {
}

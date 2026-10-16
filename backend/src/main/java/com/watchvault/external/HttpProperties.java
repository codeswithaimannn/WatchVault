package com.watchvault.external;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Settings under watchvault.http, shared by every external API client.
 *
 * @param connectTimeout how long to wait to open a connection
 * @param readTimeout    how long to wait for the response
 * @param maxRetries     extra attempts after the first one fails (0 = no retries)
 * @param retryDelay     wait before the first retry; doubles each time
 */
@Validated
@ConfigurationProperties("watchvault.http")
public record HttpProperties(
		@NotNull Duration connectTimeout,
		@NotNull Duration readTimeout,
		@PositiveOrZero int maxRetries,
		@NotNull Duration retryDelay) {
}

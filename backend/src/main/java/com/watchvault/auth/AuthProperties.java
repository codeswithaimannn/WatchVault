package com.watchvault.auth;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Settings under watchvault.auth in application.yaml. Validated at startup,
 * so a missing JWT secret stops the app instead of failing later.
 *
 * @param jwtSecret         Base64 key for signing tokens (env var WATCHVAULT_JWT_SECRET)
 * @param tokenLifetime     how long a login lasts before the user must sign in again
 * @param cookieName        name of the httpOnly cookie holding the token
 * @param secureCookie      send the cookie over HTTPS only (browsers also allow it on http://localhost)
 * @param maxFailedLogins   failed logins allowed from one IP address within the window
 * @param failedLoginWindow how long failed logins are remembered
 */
@Validated
@ConfigurationProperties("watchvault.auth")
public record AuthProperties(
		@NotBlank String jwtSecret,
		@NotNull Duration tokenLifetime,
		@NotBlank String cookieName,
		boolean secureCookie,
		@Positive int maxFailedLogins,
		@NotNull Duration failedLoginWindow) {
}

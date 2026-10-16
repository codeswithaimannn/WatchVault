package com.watchvault.auth;

import org.springframework.security.oauth2.jwt.Jwt;

/**
 * The logged-in user's id comes only from the verified token, never from the URL or body.
 * That is what stops user A from reading or changing user B's data.
 */
public final class CurrentUser {

	private CurrentUser() {
	}

	public static Long id(Jwt jwt) {
		return Long.valueOf(jwt.getSubject());
	}
}

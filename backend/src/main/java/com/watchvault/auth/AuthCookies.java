package com.watchvault.auth;

import java.time.Duration;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * Builds the cookie that carries the JWT.
 * httpOnly: page JavaScript cannot read it, so an XSS bug cannot steal the token.
 * SameSite=Lax: the browser does not send it on cross-site form posts (CSRF defence, with CSRF tokens too).
 */
@Component
public class AuthCookies {

	private static final String SAME_SITE = "Lax";

	private final AuthProperties properties;

	public AuthCookies(AuthProperties properties) {
		this.properties = properties;
	}

	public ResponseCookie loginCookie(String token) {
		return build(token, properties.tokenLifetime());
	}

	/** Same cookie with no value and Max-Age=0, which tells the browser to delete it. */
	public ResponseCookie logoutCookie() {
		return build("", Duration.ZERO);
	}

	private ResponseCookie build(String value, Duration maxAge) {
		return ResponseCookie.from(properties.cookieName(), value)
				.httpOnly(true)
				.secure(properties.secureCookie())
				.sameSite(SAME_SITE)
				.path("/")
				.maxAge(maxAge)
				.build();
	}
}

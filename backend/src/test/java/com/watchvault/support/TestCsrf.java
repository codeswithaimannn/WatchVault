package com.watchvault.support;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.test.web.servlet.request.RequestPostProcessor;

import jakarta.servlet.http.Cookie;

/**
 * Sends a CSRF token the way our frontend will: the same value in the XSRF-TOKEN cookie and
 * the X-XSRF-TOKEN header (Spring compares the two; the server stores nothing).
 *
 * Used instead of Spring's csrf() test helper, which swaps in a test-only token store for the
 * whole shared test context and stops the real XSRF-TOKEN cookie being written in later tests.
 */
public final class TestCsrf {

	private static final String TOKEN = "test-csrf-token";

	private TestCsrf() {
	}

	public static RequestPostProcessor xsrf() {
		return request -> {
			List<Cookie> cookies = new ArrayList<>();
			if (request.getCookies() != null) {
				cookies.addAll(Arrays.asList(request.getCookies()));
			}
			cookies.add(new Cookie("XSRF-TOKEN", TOKEN));
			request.setCookies(cookies.toArray(Cookie[]::new));
			request.addHeader("X-XSRF-TOKEN", TOKEN);
			return request;
		};
	}
}

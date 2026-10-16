package com.watchvault.support;

import static com.watchvault.support.TestCsrf.xsrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.servlet.http.Cookie;

/** Creates an account through the real API and returns its login cookie. */
public final class TestAccounts {

	public static final String PASSWORD = "correct horse battery";
	private static final String COOKIE_NAME = "watchvault_token";

	private TestAccounts() {
	}

	public static Cookie registerAndLogin(MockMvc mvc, String email) throws Exception {
		mvc.perform(post("/api/auth/register").with(xsrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"displayName": "Test User", "email": "%s", "password": "%s"}""".formatted(email, PASSWORD)))
				.andExpect(status().isCreated());
		return mvc.perform(post("/api/auth/login").with(xsrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"email": "%s", "password": "%s"}""".formatted(email, PASSWORD)))
				.andExpect(status().isOk())
				.andReturn().getResponse().getCookie(COOKIE_NAME);
	}
}

package com.watchvault.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static com.watchvault.support.TestCsrf.xsrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import com.watchvault.user.User;
import com.watchvault.user.UserPreferencesRepository;
import com.watchvault.user.UserRepository;

import jakarta.servlet.http.Cookie;

/**
 * Whole app against the test database, driven through HTTP with MockMvc.
 * @Transactional rolls back each test's data.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthFlowTest {

	private static final String COOKIE = "watchvault_token";
	private static final String PASSWORD = "correct horse battery";

	private final MockMvc mvc;
	private final UserRepository users;
	private final UserPreferencesRepository preferences;

	@Autowired
	AuthFlowTest(MockMvc mvc, UserRepository users, UserPreferencesRepository preferences) {
		this.mvc = mvc;
		this.users = users;
		this.preferences = preferences;
	}

	@Test
	void registerCreatesAccountWithHashedPasswordAndDefaultPreferences() throws Exception {
		register("  Sam Carter ", " New.User@Example.com ", PASSWORD)
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.email").value("new.user@example.com"))
				.andExpect(jsonPath("$.displayName").value("Sam Carter"))
				.andExpect(jsonPath("$.role").value("USER"))
				.andExpect(jsonPath("$.passwordHash").doesNotExist());

		User saved = users.findByEmail("new.user@example.com").orElseThrow();
		assertThat(saved.getPasswordHash()).startsWith("{bcrypt}").doesNotContain(PASSWORD);
		assertThat(preferences.findById(saved.getId()).orElseThrow().getRegion()).isEqualTo("GB");
	}

	@Test
	void registeringTheSameEmailTwiceIsAConflict() throws Exception {
		register("twice@example.com", PASSWORD).andExpect(status().isCreated());

		register("TWICE@example.com", PASSWORD)
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.detail").value("An account with this email already exists."));
	}

	@Test
	void invalidRegistrationReturnsFieldErrors() throws Exception {
		register("   ", "not-an-email", "short")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.displayName").value("Name is required."))
				.andExpect(jsonPath("$.errors.email").exists())
				.andExpect(jsonPath("$.errors.password").value("Password must be 12 to 72 characters."));
	}

	@Test
	void requestsWithoutCsrfTokenAreRejected() throws Exception {
		mvc.perform(post("/api/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json("csrf@example.com", PASSWORD)))
				.andExpect(status().isForbidden());
	}

	@Test
	void csrfEndpointSetsTheXsrfCookie() throws Exception {
		mvc.perform(get("/api/auth/csrf"))
				.andExpect(status().isNoContent())
				.andExpect(cookie().exists("XSRF-TOKEN"));
	}

	@Test
	void loginSetsHttpOnlyCookieThatUnlocksMe() throws Exception {
		register("alice@example.com", PASSWORD);

		Cookie token = login("alice@example.com", PASSWORD)
				.andExpect(status().isOk())
				.andExpect(cookie().httpOnly(COOKIE, true))
				.andExpect(cookie().secure(COOKIE, true))
				.andReturn().getResponse().getCookie(COOKIE);

		mvc.perform(get("/api/users/me").cookie(token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value("alice@example.com"));
	}

	@Test
	void wrongPasswordAndUnknownEmailGetTheSameAnswer() throws Exception {
		register("bob@example.com", PASSWORD);

		login("bob@example.com", "wrong password!!")
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.detail").value("Invalid email or password."));
		login("nobody@example.com", PASSWORD)
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.detail").value("Invalid email or password."));
	}

	@Test
	void meWithoutTokenIsUnauthorized() throws Exception {
		mvc.perform(get("/api/users/me")).andExpect(status().isUnauthorized());
	}

	@Test
	void tamperedTokenIsRejected() throws Exception {
		register("carol@example.com", PASSWORD);
		String token = login("carol@example.com", PASSWORD).andReturn().getResponse().getCookie(COOKIE).getValue();
		String tampered = token.substring(0, token.length() - 2) + (token.endsWith("AA") ? "BB" : "AA");

		mvc.perform(get("/api/users/me").cookie(new Cookie(COOKIE, tampered)))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void staleCookieDoesNotBlockLoggingIn() throws Exception {
		register("dave@example.com", PASSWORD);

		mvc.perform(post("/api/auth/login").with(xsrf())
				.cookie(new Cookie(COOKIE, "expired.or.garbage"))
				.contentType(MediaType.APPLICATION_JSON)
				.content(json("dave@example.com", PASSWORD)))
				.andExpect(status().isOk());
	}

	@Test
	void logoutDeletesTheCookie() throws Exception {
		mvc.perform(post("/api/auth/logout").with(xsrf()))
				.andExpect(status().isNoContent())
				.andExpect(cookie().maxAge(COOKIE, 0));
	}

	@Test
	void tooManyFailedLoginsBlocksEvenTheRightPassword() throws Exception {
		register("erin@example.com", PASSWORD);
		String attackerAddress = "203.0.113.99"; // own address, so other tests' failures don't count
		int maxFailedLogins = 5; // watchvault.auth.max-failed-logins

		for (int i = 0; i < maxFailedLogins; i++) {
			loginFrom(attackerAddress, "erin@example.com", "wrong password!!")
					.andExpect(status().isUnauthorized());
		}

		loginFrom(attackerAddress, "erin@example.com", PASSWORD)
				.andExpect(status().isTooManyRequests())
				.andExpect(jsonPath("$.detail").value("Too many failed login attempts. Please try again later."));
	}

	private ResultActions loginFrom(String clientAddress, String email, String password) throws Exception {
		return mvc.perform(post("/api/auth/login").with(xsrf())
				.with(request -> {
					request.setRemoteAddr(clientAddress);
					return request;
				})
				.contentType(MediaType.APPLICATION_JSON)
				.content(json(email, password)));
	}

	private ResultActions register(String email, String password) throws Exception {
		return register("Test User", email, password);
	}

	private ResultActions register(String displayName, String email, String password) throws Exception {
		return mvc.perform(post("/api/auth/register").with(xsrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"displayName": "%s", "email": "%s", "password": "%s"}""".formatted(displayName, email, password)));
	}

	private ResultActions login(String email, String password) throws Exception {
		return mvc.perform(post("/api/auth/login").with(xsrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(json(email, password)));
	}

	private static String json(String email, String password) {
		return """
				{"email": "%s", "password": "%s"}""".formatted(email, password);
	}
}

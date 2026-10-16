package com.watchvault.user;

import static com.watchvault.support.TestCsrf.xsrf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

import com.watchvault.support.TestAccounts;

import jakarta.servlet.http.Cookie;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PreferencesApiTest {

	private static final String URL = "/api/users/me/preferences";
	private static final String VALID_UPDATE = """
			{"region": "IE", "languages": ["en", "ja"],
			 "genres": [{"mediaType": "MOVIE", "genreId": 878}, {"mediaType": "TV", "genreId": 10765}]}""";

	private final MockMvc mvc;

	@Autowired
	PreferencesApiTest(MockMvc mvc) {
		this.mvc = mvc;
	}

	@Test
	void newAccountHasDefaultPreferences() throws Exception {
		Cookie alice = registerAndLogin("alice@example.com");

		mvc.perform(get(URL).cookie(alice))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.region").value("GB"))
				.andExpect(jsonPath("$.languages", empty()))
				.andExpect(jsonPath("$.genres", empty()));
	}

	@Test
	void updateReplacesPreferencesAndIsSaved() throws Exception {
		Cookie bob = registerAndLogin("bob@example.com");

		update(bob, VALID_UPDATE).andExpect(status().isOk());

		mvc.perform(get(URL).cookie(bob))
				.andExpect(jsonPath("$.region").value("IE"))
				.andExpect(jsonPath("$.languages", containsInAnyOrder("en", "ja")))
				.andExpect(jsonPath("$.genres.length()").value(2));
	}

	@Test
	void badShapeIsRejectedWithFieldErrors() throws Exception {
		Cookie carol = registerAndLogin("carol@example.com");

		update(carol, """
				{"region": "gb", "languages": ["english"], "genres": [{"mediaType": "MOVIE", "genreId": -1}]}""")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.region").exists());
	}

	@Test
	void unknownRegionCodeIsRejected() throws Exception {
		Cookie dave = registerAndLogin("dave@example.com");

		update(dave, """
				{"region": "ZZ", "languages": [], "genres": []}""")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("Unknown region code: ZZ"));
	}

	@Test
	void notLoggedInIsUnauthorized() throws Exception {
		mvc.perform(get(URL)).andExpect(status().isUnauthorized());
	}

	@Test
	void oneUserCannotChangeAnotherUsersPreferences() throws Exception {
		Cookie userA = registerAndLogin("user.a@example.com");
		Cookie userB = registerAndLogin("user.b@example.com");

		update(userA, VALID_UPDATE).andExpect(status().isOk());

		mvc.perform(get(URL).cookie(userB))
				.andExpect(jsonPath("$.region").value("GB"))
				.andExpect(jsonPath("$.languages", empty()));
	}

	private ResultActions update(Cookie session, String body) throws Exception {
		return mvc.perform(put(URL).cookie(session).with(xsrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(body));
	}

	private Cookie registerAndLogin(String email) throws Exception {
		return TestAccounts.registerAndLogin(mvc, email);
	}
}

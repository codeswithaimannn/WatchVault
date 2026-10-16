package com.watchvault.library;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.status;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.watchvault.support.TestCsrf.xsrf;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.ZoneId;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.watchvault.support.Fixtures;
import com.watchvault.support.TestAccounts;
import com.watchvault.user.UserRepository;

import jakarta.servlet.http.Cookie;

/** Library API end to end, with TMDB faked by WireMock. Each test's data is rolled back. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LibraryApiTest {

	@RegisterExtension
	static final WireMockExtension tmdb = WireMockExtension.newInstance()
			.options(wireMockConfig().dynamicPort())
			.build();

	@DynamicPropertySource
	static void pointTmdbAtWireMock(DynamicPropertyRegistry registry) {
		registry.add("watchvault.tmdb.base-url", tmdb::baseUrl);
		registry.add("watchvault.http.retry-delay", () -> "10ms");
	}

	private static final String MOVIE = "/api/library/movie/1001";
	private static final String SHOW = "/api/library/tv/2001";

	private final MockMvc mvc;
	private final LibraryEntryRepository entries;
	private final UserRepository users;
	private Cookie alice;

	@Autowired
	LibraryApiTest(MockMvc mvc, LibraryEntryRepository entries, UserRepository users) {
		this.mvc = mvc;
		this.entries = entries;
		this.users = users;
	}

	@BeforeEach
	void setUp() throws Exception {
		tmdb.stubFor(get(urlPathEqualTo("/movie/1001")).willReturn(okJson(Fixtures.read("tmdb/movie_details.json"))));
		tmdb.stubFor(get(urlPathEqualTo("/tv/2001")).willReturn(okJson(Fixtures.read("tmdb/tv_details.json"))));
		alice = TestAccounts.registerAndLogin(mvc, "alice@example.com");
	}

	@Test
	void notLoggedInIsUnauthorized() throws Exception {
		mvc.perform(MockMvcRequestBuilders.get("/api/library")).andExpect(status().isUnauthorized());
	}

	@Test
	void addATitleThenReadItBackWithItsTitleInfo() throws Exception {
		save(alice, MOVIE, "{\"status\": \"WATCHING\"}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("WATCHING"))
				.andExpect(jsonPath("$.title.title").value("Example Movie"));

		mvc.perform(MockMvcRequestBuilders.get(MOVIE).cookie(alice))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.mediaType").value("MOVIE"))
				.andExpect(jsonPath("$.tmdbId").value(1001));
	}

	@Test
	void completingRecordsTodaysUkDateWithRatingAndTrimmedNotes() throws Exception {
		String todayInUk = LocalDate.now(ZoneId.of("Europe/London")).toString();

		save(alice, MOVIE, """
				{"status": "COMPLETED", "rating": 8, "notes": "  Great score.  "}""")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.completedOn").value(todayInUk))
				.andExpect(jsonPath("$.rating").value(8))
				.andExpect(jsonPath("$.notes").value("Great score."));
	}

	@Test
	void listHasEveryEntryNewestFirst() throws Exception {
		save(alice, MOVIE, "{\"status\": \"PLAN_TO_WATCH\"}");
		save(alice, SHOW, "{\"status\": \"WATCHING\"}");

		mvc.perform(MockMvcRequestBuilders.get("/api/library").cookie(alice))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)))
				.andExpect(jsonPath("$[0].title.title").value("Example Anime Show"))
				.andExpect(jsonPath("$[0].title.anime").value(true))
				.andExpect(jsonPath("$[1].title.title").value("Example Movie"));
	}

	@Test
	void titleThatDoesNotExistOnTmdbCannotBeAdded() throws Exception {
		tmdb.stubFor(get(urlPathEqualTo("/movie/999999")).willReturn(status(404)));

		save(alice, "/api/library/movie/999999", "{\"status\": \"WATCHING\"}").andExpect(status().isNotFound());

		mvc.perform(MockMvcRequestBuilders.get("/api/library").cookie(alice)).andExpect(jsonPath("$", hasSize(0)));
	}

	@Test
	void invalidInputIsRejectedWithFieldMessages() throws Exception {
		save(alice, MOVIE, "{\"status\": \"WATCHING\", \"rating\": 11}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.rating").value("Rating must be from 1 to 10."));
		save(alice, MOVIE, "{\"rating\": 5}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.status").value("Status is required."));
		save(alice, MOVIE, "{\"status\": \"WATCHING\", \"notes\": \"bell\\u0007\"}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.notes").exists());
		save(alice, MOVIE, "{\"status\": \"WATCHING\", \"notes\": \"" + "x".repeat(1001) + "\"}")
				.andExpect(status().isBadRequest());
	}

	@Test
	void removingIsSafeToRepeat() throws Exception {
		save(alice, MOVIE, "{\"status\": \"WATCHING\"}");

		mvc.perform(delete(MOVIE).cookie(alice).with(xsrf())).andExpect(status().isNoContent());
		mvc.perform(MockMvcRequestBuilders.get(MOVIE).cookie(alice))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.detail").value("This title isn't in your library."));
		mvc.perform(delete(MOVIE).cookie(alice).with(xsrf())).andExpect(status().isNoContent());
	}

	@Test
	void oneUserCannotSeeOrChangeAnotherUsersLibrary() throws Exception {
		save(alice, MOVIE, "{\"status\": \"COMPLETED\", \"rating\": 9}");
		Cookie bob = TestAccounts.registerAndLogin(mvc, "bob@example.com");

		// Bob sees an empty library and gets 404 for Alice's title.
		mvc.perform(MockMvcRequestBuilders.get("/api/library").cookie(bob)).andExpect(jsonPath("$", hasSize(0)));
		mvc.perform(MockMvcRequestBuilders.get(MOVIE).cookie(bob)).andExpect(status().isNotFound());

		// Bob deleting or saving "the same" title only ever touches his own library.
		mvc.perform(delete(MOVIE).cookie(bob).with(xsrf())).andExpect(status().isNoContent());
		save(bob, MOVIE, "{\"status\": \"DROPPED\", \"rating\": 2}").andExpect(status().isOk());

		mvc.perform(MockMvcRequestBuilders.get(MOVIE).cookie(alice))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("COMPLETED"))
				.andExpect(jsonPath("$.rating").value(9));
	}

	@Test
	void libraryStillLoadsWhenTmdbIsDownAndATitleIsNotSaved() throws Exception {
		Long aliceId = users.findByEmail("alice@example.com").orElseThrow().getId();
		LibraryEntry unsaved = new LibraryEntry(aliceId, com.watchvault.media.MediaType.MOVIE, 4242);
		unsaved.update(LibraryStatus.ON_HOLD, 6, "Halfway through.", LocalDate.now());
		entries.saveAndFlush(unsaved);
		tmdb.stubFor(get(urlPathEqualTo("/movie/4242")).willReturn(status(500)));

		mvc.perform(MockMvcRequestBuilders.get("/api/library").cookie(alice))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].status").value("ON_HOLD"))
				.andExpect(jsonPath("$[0].notes").value("Halfway through."))
				.andExpect(jsonPath("$[0].title").doesNotExist());
	}

	private ResultActions save(Cookie session, String url, String body) throws Exception {
		return mvc.perform(put(url).cookie(session).with(xsrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(body));
	}
}

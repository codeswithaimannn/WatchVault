package com.watchvault.title;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.status;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.watchvault.support.Fixtures;
import com.watchvault.support.TestAccounts;

import jakarta.servlet.http.Cookie;

/**
 * The whole path: HTTP request -> controller -> catalogue (caches) -> TMDB adapter -> fake TMDB.
 * Each test uses its own query or id, because the in-memory caches live as long as the test app.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TitleApiTest {

	@RegisterExtension
	static final WireMockExtension tmdb = WireMockExtension.newInstance()
			.options(wireMockConfig().dynamicPort())
			.build();

	@DynamicPropertySource
	static void pointTmdbAtWireMock(DynamicPropertyRegistry registry) {
		registry.add("watchvault.tmdb.base-url", tmdb::baseUrl);
		registry.add("watchvault.http.retry-delay", () -> "10ms");
	}

	private final MockMvc mvc;
	private Cookie session;

	@Autowired
	TitleApiTest(MockMvc mvc) {
		this.mvc = mvc;
	}

	@BeforeEach
	void logIn() throws Exception {
		session = TestAccounts.registerAndLogin(mvc, "titles@example.com");
	}

	@Test
	void notLoggedInIsUnauthorized() throws Exception {
		mvc.perform(MockMvcRequestBuilders.get("/api/search").param("q", "anything"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void searchReturnsOurDtosAndRepeatSearchesComeFromTheCache() throws Exception {
		tmdb.stubFor(get(urlPathEqualTo("/search/multi")).willReturn(okJson(Fixtures.read("tmdb/search_multi.json"))));

		mvc.perform(authedGet("/api/search").param("q", "cache test"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.results.length()").value(3))
				.andExpect(jsonPath("$.results[0].title").value("Example Movie"))
				.andExpect(jsonPath("$.results[1].anime").value(true));
		mvc.perform(authedGet("/api/search").param("q", "  CACHE   test "))
				.andExpect(status().isOk());

		tmdb.verify(1, getRequestedFor(urlPathEqualTo("/search/multi")));
	}

	@Test
	void blankOrOverlongSearchIsRejected() throws Exception {
		mvc.perform(authedGet("/api/search").param("q", "   ")).andExpect(status().isBadRequest());
		mvc.perform(authedGet("/api/search").param("q", "x".repeat(101))).andExpect(status().isBadRequest());
		mvc.perform(authedGet("/api/search").param("q", "ok").param("page", "0")).andExpect(status().isBadRequest());
	}

	@Test
	void unknownMediaTypeIsRejected() throws Exception {
		mvc.perform(authedGet("/api/genres/anime"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("Media type must be 'movie' or 'tv'."));
	}

	@Test
	void movieDetailsAreSavedSoTheSecondRequestDoesNotCallTmdb() throws Exception {
		tmdb.stubFor(get(urlPathEqualTo("/movie/1001")).willReturn(okJson(Fixtures.read("tmdb/movie_details.json"))));

		mvc.perform(authedGet("/api/titles/movie/1001"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.title").value("Example Movie"))
				.andExpect(jsonPath("$.ukReleaseDate").value("2023-05-10"))
				.andExpect(jsonPath("$.fetchedAt").exists());
		mvc.perform(authedGet("/api/titles/movie/1001")).andExpect(status().isOk());

		tmdb.verify(1, getRequestedFor(urlPathEqualTo("/movie/1001")));
	}

	@Test
	void unknownTitleIsNotFound() throws Exception {
		tmdb.stubFor(get(urlPathEqualTo("/movie/999999")).willReturn(status(404)));

		mvc.perform(authedGet("/api/titles/movie/999999"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.detail").value("We couldn't find that title."));
	}

	@Test
	void tmdbDownWithNothingSavedIsServiceUnavailable() throws Exception {
		tmdb.stubFor(get(urlPathEqualTo("/tv/424242")).willReturn(status(500)));

		mvc.perform(authedGet("/api/titles/tv/424242"))
				.andExpect(status().isServiceUnavailable())
				.andExpect(jsonPath("$.detail").value("TMDB is not responding right now. Please try again shortly."));
	}

	@Test
	void providersCarryTheJustWatchCredit() throws Exception {
		tmdb.stubFor(get(urlPathEqualTo("/movie/1001/watch/providers"))
				.willReturn(okJson(Fixtures.read("tmdb/watch_providers.json"))));

		mvc.perform(authedGet("/api/titles/movie/1001/providers"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.region").value("GB"))
				.andExpect(jsonPath("$.stream[0].name").value("First Service"))
				.andExpect(jsonPath("$.attribution").value("Streaming data from JustWatch"));
	}

	@Test
	void seasonEpisodesIncludeRuntimes() throws Exception {
		tmdb.stubFor(get(urlPathEqualTo("/tv/2001/season/1")).willReturn(okJson(Fixtures.read("tmdb/tv_season.json"))));

		mvc.perform(authedGet("/api/titles/tv/2001/seasons/1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.episodes[0].runtimeMinutes").value(24))
				.andExpect(jsonPath("$.episodes[1].runtimeMinutes").doesNotExist());
	}

	private MockHttpServletRequestBuilder authedGet(String url) {
		return MockMvcRequestBuilders.get(url).cookie(session);
	}
}

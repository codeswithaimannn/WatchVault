package com.watchvault.external.tmdb;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.status;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.watchvault.external.ExternalApiUnavailableException;
import com.watchvault.external.ExternalCalls;
import com.watchvault.external.ExternalResourceNotFoundException;
import com.watchvault.external.HttpProperties;

/**
 * TmdbClient against a fake TMDB (WireMock) on a random local port. No Spring needed.
 * Response bodies are tiny hand-written JSON, not real TMDB data.
 */
class TmdbClientTest {

	@RegisterExtension
	static final WireMockExtension tmdb = WireMockExtension.newInstance()
			.options(wireMockConfig().dynamicPort())
			.build();

	private static final String PATH = "/genre/movie/list";
	private static final int MAX_RETRIES = 2;
	private static final Duration READ_TIMEOUT = Duration.ofMillis(500);

	private TmdbClient client;

	/** Stand-in response type for these tests. */
	record Probe(String name) {
	}

	@BeforeEach
	void setUp() {
		// Tiny retry delay so the tests stay fast.
		HttpProperties http = new HttpProperties(Duration.ofSeconds(1), READ_TIMEOUT, MAX_RETRIES, Duration.ofMillis(10));
		TmdbProperties properties = new TmdbProperties(tmdb.baseUrl(), "test-token", "en-GB", "GB");
		client = new TmdbClient(properties, http, new ExternalCalls(http));
	}

	@Test
	void sendsBearerTokenLanguageAndEncodedQueryParameters() {
		tmdb.stubFor(get(urlPathEqualTo(PATH)).willReturn(okJson("{\"name\": \"ok\"}")));

		Probe result = client.get(PATH, Map.of("query", "fast & furious"), Probe.class);

		assertThat(result.name()).isEqualTo("ok");
		tmdb.verify(getRequestedFor(urlPathEqualTo(PATH))
				.withHeader("Authorization", equalTo("Bearer test-token"))
				.withQueryParam("language", equalTo("en-GB"))
				.withQueryParam("query", equalTo("fast & furious")));
	}

	@Test
	void retriesAfterTooManyRequestsAndThenSucceeds() {
		tmdb.stubFor(get(urlPathEqualTo(PATH)).inScenario("busy").whenScenarioStateIs(STARTED)
				.willReturn(status(429)).willSetStateTo("calm"));
		tmdb.stubFor(get(urlPathEqualTo(PATH)).inScenario("busy").whenScenarioStateIs("calm")
				.willReturn(okJson("{\"name\": \"ok\"}")));

		Probe result = client.get(PATH, Map.of(), Probe.class);

		assertThat(result.name()).isEqualTo("ok");
		tmdb.verify(2, getRequestedFor(urlPathEqualTo(PATH)));
	}

	@Test
	void keepsFailingServerErrorsBecomeUnavailableAfterAllRetries() {
		tmdb.stubFor(get(urlPathEqualTo(PATH)).willReturn(status(500)));

		assertThatThrownBy(() -> client.get(PATH, Map.of(), Probe.class))
				.isInstanceOf(ExternalApiUnavailableException.class)
				.hasMessageContaining("TMDB is not responding");
		tmdb.verify(1 + MAX_RETRIES, getRequestedFor(urlPathEqualTo(PATH)));
	}

	@Test
	void notFoundIsReportedAndNotRetried() {
		tmdb.stubFor(get(urlPathEqualTo(PATH)).willReturn(status(404)));

		assertThatThrownBy(() -> client.get(PATH, Map.of(), Probe.class))
				.isInstanceOf(ExternalResourceNotFoundException.class);
		tmdb.verify(1, getRequestedFor(urlPathEqualTo(PATH)));
	}

	@Test
	void badTokenIsUnavailableAndNotRetried() {
		tmdb.stubFor(get(urlPathEqualTo(PATH)).willReturn(status(401)));

		assertThatThrownBy(() -> client.get(PATH, Map.of(), Probe.class))
				.isInstanceOf(ExternalApiUnavailableException.class);
		tmdb.verify(1, getRequestedFor(urlPathEqualTo(PATH)));
	}

	@Test
	void tooSlowResponseCountsAsUnavailable() {
		int slowerThanTimeoutMs = (int) READ_TIMEOUT.toMillis() * 3;
		tmdb.stubFor(get(urlPathEqualTo(PATH))
				.willReturn(aResponse().withFixedDelay(slowerThanTimeoutMs).withBody("{}")));

		assertThatThrownBy(() -> client.get(PATH, Map.of(), Probe.class))
				.isInstanceOf(ExternalApiUnavailableException.class);
	}
}

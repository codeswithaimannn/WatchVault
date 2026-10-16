package com.watchvault.external.tvmaze;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.permanentRedirect;
import static com.github.tomakehurst.wiremock.client.WireMock.status;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.watchvault.external.ExternalApiUnavailableException;
import com.watchvault.external.ExternalCalls;
import com.watchvault.external.HttpProperties;
import com.watchvault.schedule.DaySchedule;
import com.watchvault.schedule.EpisodeAiring;
import com.watchvault.schedule.EpisodeAiring.Source;
import com.watchvault.schedule.TvShowLink;
import com.watchvault.support.Fixtures;

/** TvmazeService against WireMock with hand-written fixtures in TVmaze's format. */
class TvmazeServiceTest {

	@RegisterExtension
	static final WireMockExtension tvmaze = WireMockExtension.newInstance()
			.options(wireMockConfig().dynamicPort())
			.build();

	private static final LocalDate DAY = LocalDate.of(2026, 10, 15);

	private TvmazeService service;

	@BeforeEach
	void setUp() {
		HttpProperties http = new HttpProperties(Duration.ofSeconds(1), Duration.ofSeconds(2), 1, Duration.ofMillis(10));
		TvmazeClient client = new TvmazeClient(new TvmazeProperties(tvmaze.baseUrl(), "GB"), http, new ExternalCalls(http));
		service = new TvmazeService(client);
	}

	@Test
	void lookupFollowsTvmazesRedirectToTheShow() {
		tvmaze.stubFor(get(urlPathEqualTo("/lookup/shows"))
				.willReturn(permanentRedirect(tvmaze.baseUrl() + "/shows/501")));
		stub("/shows/501", "show.json");

		TvShowLink link = service.lookupByImdb("tt0000002").orElseThrow();

		assertThat(link.tvmazeId()).isEqualTo(501);
		assertThat(link.tvdbId()).isEqualTo(300001);
		assertThat(link.channel()).isEqualTo("Example Stream"); // webChannel, as there is no network
		tvmaze.verify(getRequestedFor(urlPathEqualTo("/lookup/shows")).withQueryParam("imdb", equalTo("tt0000002")));
	}

	@Test
	void unknownShowIsEmptyNotAnError() {
		tvmaze.stubFor(get(urlPathEqualTo("/lookup/shows")).willReturn(status(404)));

		assertThat(service.lookupByImdb("tt9999999")).isEmpty();
		assertThat(service.lookupByTvdb(999)).isEmpty();
	}

	@Test
	void episodesOfAStreamingShowHaveDatesButNoTimes() {
		stub("/shows/501", "show.json");
		stub("/shows/501/episodes", "episodes.json");

		List<EpisodeAiring> episodes = service.episodes(501);

		EpisodeAiring first = episodes.get(0);
		assertThat(first.airDate()).isEqualTo(LocalDate.of(2022, 2, 18));
		assertThat(first.airTime()).isNull(); // "" and a 12:00 placeholder airstamp: no time shown
		assertThat(first.runtimeMinutes()).isEqualTo(57);
		assertThat(first.source()).isEqualTo(Source.STREAMING);

		EpisodeAiring unannounced = episodes.get(1);
		assertThat(unannounced.airDate()).isNull();
		assertThat(unannounced.runtimeMinutes()).isNull();
	}

	@Test
	void scheduleCombinesBroadcastAndStreamingWithoutDuplicates() {
		stub("/schedule", "schedule_broadcast.json");
		stub("/schedule/web", "schedule_web.json");

		DaySchedule schedule = service.schedule(DAY);

		assertThat(schedule.episodes()).extracting(EpisodeAiring::showName)
				.containsExactly("Example Soap", "Example Web Series"); // the soap appears once
		assertThat(schedule.attribution()).isEqualTo("Schedule data from TVmaze (CC BY-SA 4.0)");
		tvmaze.verify(getRequestedFor(urlPathEqualTo("/schedule"))
				.withQueryParam("country", equalTo("GB"))
				.withQueryParam("date", equalTo("2026-10-15")));
		tvmaze.verify(getRequestedFor(urlPathEqualTo("/schedule/web"))
				.withQueryParam("country", equalTo("GB")));
	}

	@Test
	void broadcastKeepsItsTimeButStreamingNeverHasOne() {
		stub("/schedule", "schedule_broadcast.json");
		stub("/schedule/web", "schedule_web.json");

		List<EpisodeAiring> episodes = service.schedule(DAY).episodes();

		EpisodeAiring soap = episodes.get(0);
		assertThat(soap.source()).isEqualTo(Source.BROADCAST);
		assertThat(soap.airTime()).isEqualTo(LocalTime.of(19, 30));
		assertThat(soap.channel()).isEqualTo("Example One");

		EpisodeAiring webSeries = episodes.get(1);
		assertThat(webSeries.source()).isEqualTo(Source.STREAMING);
		assertThat(webSeries.airTime()).isNull(); // TVmaze said 08:00, but streaming times are unreliable
		assertThat(webSeries.airDate()).isEqualTo(DAY);
	}

	@Test
	void tooManyRequestsIsRetried() {
		tvmaze.stubFor(get(urlPathEqualTo("/shows/501")).inScenario("limit").whenScenarioStateIs(STARTED)
				.willReturn(status(429)).willSetStateTo("ok"));
		tvmaze.stubFor(get(urlPathEqualTo("/shows/501")).inScenario("limit").whenScenarioStateIs("ok")
				.willReturn(okJson(Fixtures.read("tvmaze/show.json"))));
		stub("/shows/501/episodes", "episodes.json");

		assertThat(service.episodes(501)).hasSize(2);
	}

	@Test
	void serverErrorsBecomeUnavailable() {
		tvmaze.stubFor(get(urlPathEqualTo("/schedule")).willReturn(status(503)));

		assertThatThrownBy(() -> service.schedule(DAY))
				.isInstanceOf(ExternalApiUnavailableException.class)
				.hasMessageContaining("TVmaze");
	}

	private static void stub(String path, String fixture) {
		tvmaze.stubFor(get(urlPathEqualTo(path)).willReturn(okJson(Fixtures.read("tvmaze/" + fixture))));
	}
}

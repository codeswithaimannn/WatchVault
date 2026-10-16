package com.watchvault.external.tmdb;

import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.watchvault.external.ExternalCalls;
import com.watchvault.external.ExternalHttp;
import com.watchvault.external.HttpProperties;

/**
 * Low-level TMDB access: authentication, language, retries and error handling.
 * It returns TMDB's own response shapes; TmdbService turns them into our DTOs.
 */
@Component
public class TmdbClient {

	static final String API_NAME = "TMDB";

	private final RestClient restClient;
	private final ExternalCalls externalCalls;
	private final TmdbProperties properties;

	public TmdbClient(TmdbProperties properties, HttpProperties http, ExternalCalls externalCalls) {
		this.properties = properties;
		this.externalCalls = externalCalls;
		this.restClient = ExternalHttp.restClientBuilder(http)
				.baseUrl(properties.baseUrl())
				.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.readToken())
				.defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
				.build();
	}

	/**
	 * GET a TMDB path such as "/movie/693134". The configured language is always added;
	 * other query parameters come from {@code queryParams} and are URL-encoded for us.
	 */
	<T> T get(String path, Map<String, ?> queryParams, Class<T> responseType) {
		return externalCalls.call(API_NAME, () -> restClient.get()
				.uri(uri -> {
					uri.path(path).queryParam("language", properties.language());
					queryParams.forEach(uri::queryParam);
					return uri.build();
				})
				.retrieve()
				.body(responseType));
	}

	String region() {
		return properties.region();
	}
}

package com.watchvault.external.tvmaze;

import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.watchvault.external.ExternalCalls;
import com.watchvault.external.ExternalHttp;
import com.watchvault.external.HttpProperties;

/** Low-level TVmaze access: retries and error handling. Returns TVmaze's own shapes. */
@Component
public class TvmazeClient {

	static final String API_NAME = "TVmaze";

	private final RestClient restClient;
	private final ExternalCalls externalCalls;
	private final TvmazeProperties properties;

	public TvmazeClient(TvmazeProperties properties, HttpProperties http, ExternalCalls externalCalls) {
		this.properties = properties;
		this.externalCalls = externalCalls;
		this.restClient = ExternalHttp.restClientBuilder(http)
				.baseUrl(properties.baseUrl())
				.defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
				.build();
	}

	<T> T get(String path, Map<String, ?> queryParams, Class<T> responseType) {
		return externalCalls.call(API_NAME, () -> restClient.get()
				.uri(uri -> {
					uri.path(path);
					queryParams.forEach(uri::queryParam);
					return uri.build();
				})
				.retrieve()
				.body(responseType));
	}

	String country() {
		return properties.country();
	}
}

package com.watchvault.external;

import java.net.http.HttpClient;

import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/** Builds RestClients for external APIs: Java's built-in HTTP client, with timeouts and redirects. */
public final class ExternalHttp {

	private ExternalHttp() {
	}

	public static RestClient.Builder restClientBuilder(HttpProperties http) {
		HttpClient httpClient = HttpClient.newBuilder()
				.connectTimeout(http.connectTimeout())
				// TVmaze answers some lookups with a 301 redirect to the real resource.
				.followRedirects(HttpClient.Redirect.NORMAL)
				.build();
		JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
		requestFactory.setReadTimeout(http.readTimeout());
		return RestClient.builder().requestFactory(requestFactory);
	}
}

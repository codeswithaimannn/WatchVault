package com.watchvault.external;

import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.retry.RetryException;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

/**
 * Runs a call to an external API with retries, and turns failures into our own exceptions.
 *
 * Retried (the problem is probably temporary): 429 Too Many Requests, 5xx server errors,
 * timeouts and connection failures. Not retried: 404 and other 4xx, because asking again
 * gives the same answer. We don't count requests: TMDB sends no "remaining" headers, so
 * a 429 is the real signal (see DECISIONS.md).
 */
@Component
public class ExternalCalls {

	private static final Logger log = LoggerFactory.getLogger(ExternalCalls.class);
	private static final double BACKOFF_MULTIPLIER = 2.0;

	private final RetryTemplate retryTemplate;

	public ExternalCalls(HttpProperties http) {
		RetryPolicy policy = RetryPolicy.builder()
				.maxRetries(http.maxRetries())
				.delay(http.retryDelay())
				.multiplier(BACKOFF_MULTIPLIER) // e.g. 300ms, then 600ms
				.predicate(ExternalCalls::isWorthRetrying)
				.build();
		this.retryTemplate = new RetryTemplate(policy);
	}

	public <T> T call(String apiName, Supplier<T> request) {
		try {
			return retryTemplate.execute(request::get);
		} catch (RetryException e) {
			Throwable failure = e.getLastException();
			if (failure instanceof HttpClientErrorException.NotFound) {
				throw new ExternalResourceNotFoundException();
			}
			// Details go to our log only; the user gets a safe, general message.
			log.warn("{} call failed after {} retries: {}", apiName, e.getRetryCount(), failure.toString());
			throw new ExternalApiUnavailableException(apiName);
		}
	}

	static boolean isWorthRetrying(Throwable failure) {
		return failure instanceof HttpClientErrorException.TooManyRequests
				|| failure instanceof HttpServerErrorException
				|| failure instanceof ResourceAccessException;
	}
}

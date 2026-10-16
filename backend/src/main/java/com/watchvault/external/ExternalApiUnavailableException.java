package com.watchvault.external;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

/** 503: an external API is down, too busy, or misconfigured, and retries did not help. */
public class ExternalApiUnavailableException extends ErrorResponseException {

	public ExternalApiUnavailableException(String apiName) {
		super(HttpStatus.SERVICE_UNAVAILABLE, ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
				apiName + " is not responding right now. Please try again shortly."), null);
	}
}

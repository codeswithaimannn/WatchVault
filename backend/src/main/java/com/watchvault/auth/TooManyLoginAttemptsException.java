package com.watchvault.auth;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

/** 429 Too Many Requests. Says "later", not a time, because the exact wait is not shown. */
public class TooManyLoginAttemptsException extends ErrorResponseException {

	public TooManyLoginAttemptsException() {
		super(HttpStatus.TOO_MANY_REQUESTS, ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS,
				"Too many failed login attempts. Please try again later."), null);
	}
}

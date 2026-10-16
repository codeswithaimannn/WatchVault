package com.watchvault.auth;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

/** 409 Conflict. Rendered by GlobalExceptionHandler's base class. */
public class EmailAlreadyRegisteredException extends ErrorResponseException {

	public EmailAlreadyRegisteredException() {
		super(HttpStatus.CONFLICT, ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
				"An account with this email already exists."), null);
	}
}

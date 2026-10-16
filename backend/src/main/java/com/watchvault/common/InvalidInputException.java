package com.watchvault.common;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

/** 400 for input that passes annotation checks but breaks a rule checked in code. */
public class InvalidInputException extends ErrorResponseException {

	public InvalidInputException(String safeMessage) {
		super(HttpStatus.BAD_REQUEST, ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, safeMessage), null);
	}
}

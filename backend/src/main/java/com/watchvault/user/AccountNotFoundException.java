package com.watchvault.user;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

/** 404 for a valid token whose account has since been deleted. */
public class AccountNotFoundException extends ErrorResponseException {

	public AccountNotFoundException() {
		super(HttpStatus.NOT_FOUND, ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND,
				"Account not found."), null);
	}
}

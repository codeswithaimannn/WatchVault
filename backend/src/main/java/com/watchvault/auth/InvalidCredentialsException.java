package com.watchvault.auth;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

/**
 * 401. Same message whether the email is unknown or the password is wrong,
 * so the login form cannot be used to find out who has an account.
 */
public class InvalidCredentialsException extends ErrorResponseException {

	public InvalidCredentialsException() {
		super(HttpStatus.UNAUTHORIZED, ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED,
				"Invalid email or password."), null);
	}
}

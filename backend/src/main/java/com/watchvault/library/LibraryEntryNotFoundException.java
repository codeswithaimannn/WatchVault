package com.watchvault.library;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

/** 404: this title is not in the logged-in user's library. */
public class LibraryEntryNotFoundException extends ErrorResponseException {

	public LibraryEntryNotFoundException() {
		super(HttpStatus.NOT_FOUND, ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND,
				"This title isn't in your library."), null);
	}
}

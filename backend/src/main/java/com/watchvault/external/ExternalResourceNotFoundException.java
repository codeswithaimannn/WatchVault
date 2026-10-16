package com.watchvault.external;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

/** 404: the external API says this item does not exist (e.g. an unknown TMDB id). */
public class ExternalResourceNotFoundException extends ErrorResponseException {

	public ExternalResourceNotFoundException() {
		super(HttpStatus.NOT_FOUND, ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND,
				"We couldn't find that title."), null);
	}
}

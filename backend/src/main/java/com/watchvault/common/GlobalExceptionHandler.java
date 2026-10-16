package com.watchvault.common;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Turns exceptions into safe JSON error bodies (RFC 9457 "problem details").
 * Our own exceptions extend ErrorResponseException and carry their status and message,
 * which the base class already knows how to render. This class adds two things:
 * per-field validation messages, and a generic 500 that never leaks internals.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		Map<String, String> fieldErrors = new LinkedHashMap<>();
		addFieldErrors(ex.getBindingResult().getFieldErrors(), fieldErrors);
		return invalidFields(fieldErrors);
	}

	/**
	 * Raised instead of the one above when a method validates both its URL parameters
	 * (e.g. @Positive id) and its body (@Valid): Spring then checks the whole method at once.
	 * Same response shape either way, so the frontend can rely on "errors".
	 */
	@Override
	protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		Map<String, String> fieldErrors = new LinkedHashMap<>();
		for (ParameterValidationResult result : ex.getParameterValidationResults()) {
			if (result instanceof ParameterErrors bodyErrors) {
				addFieldErrors(bodyErrors.getFieldErrors(), fieldErrors); // fields inside a request body
			} else {
				String parameter = result.getMethodParameter().getParameterName(); // e.g. tmdbId, q
				result.getResolvableErrors().stream().findFirst()
						.ifPresent(error -> fieldErrors.putIfAbsent(parameter, error.getDefaultMessage()));
			}
		}
		return invalidFields(fieldErrors);
	}

	private static void addFieldErrors(List<FieldError> errors, Map<String, String> into) {
		for (FieldError error : errors) {
			into.putIfAbsent(error.getField(), error.getDefaultMessage());
		}
	}

	private static ResponseEntity<Object> invalidFields(Map<String, String> fieldErrors) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Some fields are invalid.");
		problem.setProperty("errors", fieldErrors);
		return ResponseEntity.badRequest().body(problem);
	}

	/** Anything unexpected: log the details for us, show the user nothing internal. */
	@ExceptionHandler(Exception.class)
	ProblemDetail handleUnexpected(Exception ex) {
		log.error("Unexpected error", ex);
		return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
				"Something went wrong. Please try again.");
	}
}

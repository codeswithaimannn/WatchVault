package com.watchvault.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** No password length rules here: login should not reveal the password policy. */
public record LoginRequest(

		@NotBlank(message = "Email is required.")
		@Size(max = 254, message = "Email is too long.")
		String email,

		@NotBlank(message = "Password is required.")
		String password) {

	public LoginRequest {
		email = email == null ? null : email.trim();
	}
}

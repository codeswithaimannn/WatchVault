package com.watchvault.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

		@NotBlank(message = "Email is required.")
		@Email(message = "Enter a valid email address.")
		@Size(max = 254, message = "Email is too long.")
		String email,

		@NotBlank(message = "Password is required.")
		@Size(min = PasswordRules.MIN_LENGTH, max = PasswordRules.BCRYPT_MAX_BYTES,
				message = "Password must be 12 to 72 characters.")
		String password) {

	/** Trim before validation runs, so " bob@example.com" is not rejected as invalid. */
	public RegisterRequest {
		email = email == null ? null : email.trim();
	}
}

package com.watchvault.user;

/** What the API shows about a user. Never includes the password hash. */
public record UserResponse(Long id, String email, Role role) {

	public static UserResponse from(User user) {
		return new UserResponse(user.getId(), user.getEmail(), user.getRole());
	}
}

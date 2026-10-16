package com.watchvault.user;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.watchvault.auth.CurrentUser;

/** "/me" endpoints: there is no user id in the path to tamper with. */
@RestController
@RequestMapping("/api/users/me")
public class UserController {

	private final UserService userService;

	public UserController(UserService userService) {
		this.userService = userService;
	}

	@GetMapping
	public UserResponse me(@AuthenticationPrincipal Jwt jwt) {
		return UserResponse.from(userService.getById(CurrentUser.id(jwt)));
	}
}

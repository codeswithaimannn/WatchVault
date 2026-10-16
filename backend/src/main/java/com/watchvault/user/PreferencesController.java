package com.watchvault.user;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.watchvault.auth.CurrentUser;

import jakarta.validation.Valid;

/** The user id always comes from the token, so users can only see and change their own preferences. */
@RestController
@RequestMapping("/api/users/me/preferences")
public class PreferencesController {

	private final UserPreferencesService preferencesService;

	public PreferencesController(UserPreferencesService preferencesService) {
		this.preferencesService = preferencesService;
	}

	@GetMapping
	public PreferencesResponse get(@AuthenticationPrincipal Jwt jwt) {
		return preferencesService.get(CurrentUser.id(jwt));
	}

	@PutMapping
	public PreferencesResponse update(@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody UpdatePreferencesRequest request) {
		return preferencesService.update(CurrentUser.id(jwt), request);
	}
}

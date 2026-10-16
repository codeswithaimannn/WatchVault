package com.watchvault.auth;

import java.net.URI;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.watchvault.user.User;
import com.watchvault.user.UserResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final RegistrationService registrationService;
	private final LoginService loginService;
	private final TokenService tokenService;
	private final AuthCookies authCookies;

	public AuthController(RegistrationService registrationService, LoginService loginService,
			TokenService tokenService, AuthCookies authCookies) {
		this.registrationService = registrationService;
		this.loginService = loginService;
		this.tokenService = tokenService;
		this.authCookies = authCookies;
	}

	/** The frontend calls this once on start-up; reading the token makes Spring set the XSRF-TOKEN cookie. */
	@GetMapping("/csrf")
	public ResponseEntity<Void> csrf(CsrfToken csrfToken) {
		csrfToken.getToken();
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/register")
	public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
		User user = registrationService.register(request.email(), request.displayName(), request.password());
		return ResponseEntity.created(URI.create("/api/users/me")).body(UserResponse.from(user));
	}

	@PostMapping("/login")
	public ResponseEntity<UserResponse> login(@Valid @RequestBody LoginRequest request,
			HttpServletRequest httpRequest) {
		// Direct client IP. Behind a proxy this needs X-Forwarded-For handling (deploy milestone).
		String clientAddress = httpRequest.getRemoteAddr();
		User user = loginService.authenticate(request.email(), request.password(), clientAddress);
		String token = tokenService.issueToken(user);
		return ResponseEntity.ok()
				.header(HttpHeaders.SET_COOKIE, authCookies.loginCookie(token).toString())
				.body(UserResponse.from(user));
	}

	/**
	 * Deletes the cookie. The JWT itself stays valid until it expires (tokens are stateless),
	 * which is why the lifetime is kept short.
	 */
	@PostMapping("/logout")
	public ResponseEntity<Void> logout() {
		return ResponseEntity.noContent()
				.header(HttpHeaders.SET_COOKIE, authCookies.logoutCookie().toString())
				.build();
	}
}

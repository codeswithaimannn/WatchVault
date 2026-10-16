package com.watchvault.auth;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.util.WebUtils;

import jakarta.servlet.http.Cookie;

@Configuration
public class SecurityConfig {

	private static final String AUTH_PATHS = "/api/auth/";

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, BearerTokenResolver cookieTokenResolver)
			throws Exception {
		http
				// The browser sends our cookie automatically, so state-changing requests also need
				// a CSRF token: Spring sets an XSRF-TOKEN cookie, the frontend echoes it in X-XSRF-TOKEN.
				.csrf(csrf -> csrf.spa())
				// No server-side sessions: every request carries its own JWT.
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(HttpMethod.GET, "/api/auth/csrf").permitAll()
						.requestMatchers(HttpMethod.POST,
								"/api/auth/register", "/api/auth/login", "/api/auth/logout").permitAll()
						.requestMatchers("/actuator/health", "/error").permitAll()
						.anyRequest().authenticated())
				// Spring checks the JWT on every request: signature, expiry, issuer.
				.oauth2ResourceServer(resourceServer -> resourceServer
						.bearerTokenResolver(cookieTokenResolver)
						.jwt(jwt -> jwt.jwtAuthenticationConverter(rolesConverter())));
		return http.build();
	}

	/**
	 * Reads the JWT from our httpOnly cookie instead of an Authorization header.
	 * Skipped for /api/auth/** so an expired cookie can never block logging in again
	 * (Spring rejects a bad token even on permitAll paths).
	 */
	@Bean
	BearerTokenResolver cookieTokenResolver(AuthProperties properties) {
		return request -> {
			if (request.getRequestURI().startsWith(AUTH_PATHS)) {
				return null;
			}
			Cookie cookie = WebUtils.getCookie(request, properties.cookieName());
			return cookie == null || cookie.getValue().isBlank() ? null : cookie.getValue();
		};
	}

	/**
	 * Hashes as BCrypt and stores it as "{bcrypt}$2a$...". The prefix lets us switch
	 * algorithm later while old hashes keep working.
	 */
	@Bean
	PasswordEncoder passwordEncoder() {
		return PasswordEncoderFactories.createDelegatingPasswordEncoder();
	}

	/** Turns the "roles" claim (["USER"]) into Spring authorities (ROLE_USER). */
	private JwtAuthenticationConverter rolesConverter() {
		JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
		authorities.setAuthoritiesClaimName(TokenService.ROLES_CLAIM);
		authorities.setAuthorityPrefix("ROLE_");
		JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
		converter.setJwtGrantedAuthoritiesConverter(authorities);
		return converter;
	}
}

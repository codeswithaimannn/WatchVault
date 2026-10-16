package com.watchvault.auth;

import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.watchvault.user.User;
import com.watchvault.user.UserRepository;

/** Checks an email and password. Issuing the token and cookie is the controller's job. */
@Service
public class LoginService {

	private final UserRepository users;
	private final PasswordEncoder passwordEncoder;
	private final LoginRateLimiter rateLimiter;

	/** Compared against when the email is unknown, so both cases take the same (slow) time. */
	private final String dummyHash;

	public LoginService(UserRepository users, PasswordEncoder passwordEncoder, LoginRateLimiter rateLimiter) {
		this.users = users;
		this.passwordEncoder = passwordEncoder;
		this.rateLimiter = rateLimiter;
		this.dummyHash = passwordEncoder.encode("dummy-password-for-timing");
	}

	/** Checked before the password, so a blocked client is refused even with the right one. */
	@Transactional(readOnly = true)
	public User authenticate(String email, String password, String clientAddress) {
		if (rateLimiter.isBlocked(clientAddress)) {
			throw new TooManyLoginAttemptsException();
		}
		Optional<User> user = checkCredentials(email, password);
		if (user.isEmpty()) {
			rateLimiter.recordFailure(clientAddress);
			throw new InvalidCredentialsException();
		}
		rateLimiter.recordSuccess(clientAddress);
		return user.get();
	}

	private Optional<User> checkCredentials(String email, String password) {
		if (PasswordRules.exceedsBcryptLimit(password)) {
			return Optional.empty();
		}
		Optional<User> user = users.findByEmail(User.normalizeEmail(email));
		String hash = user.map(User::getPasswordHash).orElse(dummyHash);
		boolean passwordMatches = passwordEncoder.matches(password, hash);
		return passwordMatches ? user : Optional.empty();
	}
}

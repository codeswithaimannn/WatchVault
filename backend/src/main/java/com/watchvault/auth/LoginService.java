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

	/** Compared against when the email is unknown, so both cases take the same (slow) time. */
	private final String dummyHash;

	public LoginService(UserRepository users, PasswordEncoder passwordEncoder) {
		this.users = users;
		this.passwordEncoder = passwordEncoder;
		this.dummyHash = passwordEncoder.encode("dummy-password-for-timing");
	}

	@Transactional(readOnly = true)
	public User authenticate(String email, String password) {
		if (PasswordRules.exceedsBcryptLimit(password)) {
			throw new InvalidCredentialsException();
		}
		Optional<User> user = users.findByEmail(User.normalizeEmail(email));
		String hash = user.map(User::getPasswordHash).orElse(dummyHash);
		boolean passwordMatches = passwordEncoder.matches(password, hash);
		if (user.isEmpty() || !passwordMatches) {
			throw new InvalidCredentialsException();
		}
		return user.get();
	}
}

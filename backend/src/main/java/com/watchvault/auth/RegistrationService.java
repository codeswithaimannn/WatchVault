package com.watchvault.auth;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.watchvault.common.InvalidInputException;
import com.watchvault.user.User;
import com.watchvault.user.UserPreferences;
import com.watchvault.user.UserPreferencesRepository;
import com.watchvault.user.UserRepository;

@Service
public class RegistrationService {

	private final UserRepository users;
	private final UserPreferencesRepository preferences;
	private final PasswordEncoder passwordEncoder;

	public RegistrationService(UserRepository users, UserPreferencesRepository preferences,
			PasswordEncoder passwordEncoder) {
		this.users = users;
		this.preferences = preferences;
		this.passwordEncoder = passwordEncoder;
	}

	/** Creates the account and its default preferences together: both or neither. */
	@Transactional
	public User register(String email, String password) {
		if (PasswordRules.exceedsBcryptLimit(password)) {
			throw new InvalidInputException("Password is too long.");
		}
		String normalizedEmail = User.normalizeEmail(email);
		if (users.existsByEmail(normalizedEmail)) {
			throw new EmailAlreadyRegisteredException();
		}
		try {
			User user = users.saveAndFlush(new User(normalizedEmail, passwordEncoder.encode(password)));
			preferences.save(new UserPreferences(user));
			return user;
		} catch (DataIntegrityViolationException e) {
			// Two sign-ups with the same email at the same moment: the unique constraint caught it.
			throw new EmailAlreadyRegisteredException();
		}
	}
}

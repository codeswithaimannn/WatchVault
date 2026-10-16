package com.watchvault.auth;

import java.nio.charset.StandardCharsets;

/** Password limits shared by registration and login. */
final class PasswordRules {

	/** Length over complexity rules, in line with NCSC and NIST advice. */
	static final int MIN_LENGTH = 12;

	/** BCrypt only reads the first 72 bytes; Spring refuses longer input rather than silently cutting it. */
	static final int BCRYPT_MAX_BYTES = 72;

	private PasswordRules() {
	}

	/** Counts bytes, not characters: "é" is 2 bytes in UTF-8, an emoji is 4. */
	static boolean exceedsBcryptLimit(String password) {
		return password.getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAX_BYTES;
	}
}

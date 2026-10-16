package com.watchvault.auth;

import java.util.Base64;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/**
 * Signing and checking our own JWTs with one shared secret (HMAC-SHA256).
 * One key is enough because the same app both issues and checks the tokens.
 */
@Configuration
public class JwtConfig {

	/** HS256 needs a key of at least 256 bits. */
	private static final int MIN_KEY_BYTES = 32;

	static final String ISSUER = "watchvault";

	@Bean
	SecretKey jwtSigningKey(AuthProperties properties) {
		byte[] keyBytes = Base64.getDecoder().decode(properties.jwtSecret());
		if (keyBytes.length < MIN_KEY_BYTES) {
			throw new IllegalStateException(
					"watchvault.auth.jwt-secret must decode to at least " + MIN_KEY_BYTES + " bytes");
		}
		return new SecretKeySpec(keyBytes, "HmacSHA256");
	}

	@Bean
	JwtEncoder jwtEncoder(SecretKey jwtSigningKey) {
		return NimbusJwtEncoder.withSecretKey(jwtSigningKey).build();
	}

	@Bean
	JwtDecoder jwtDecoder(SecretKey jwtSigningKey) {
		NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(jwtSigningKey)
				.macAlgorithm(MacAlgorithm.HS256)
				.build();
		// Checks the signature, the expiry time, and that we issued it.
		decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(ISSUER));
		return decoder;
	}
}

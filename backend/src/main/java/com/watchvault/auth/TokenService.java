package com.watchvault.auth;

import java.time.Instant;
import java.util.List;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import com.watchvault.user.User;

/** Issues a signed JWT after a successful login. */
@Service
public class TokenService {

	/** Claim holding the user's roles; SecurityConfig turns it into ROLE_ authorities. */
	static final String ROLES_CLAIM = "roles";

	private final JwtEncoder encoder;
	private final AuthProperties properties;

	public TokenService(JwtEncoder encoder, AuthProperties properties) {
		this.encoder = encoder;
		this.properties = properties;
	}

	public String issueToken(User user) {
		Instant now = Instant.now();
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer(JwtConfig.ISSUER)
				.subject(user.getId().toString()) // who: the user id, never the email
				.issuedAt(now)
				.expiresAt(now.plus(properties.tokenLifetime()))
				.claim(ROLES_CLAIM, List.of(user.getRole().name()))
				.build();
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
	}
}

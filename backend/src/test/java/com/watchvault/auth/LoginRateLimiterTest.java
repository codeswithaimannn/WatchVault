package com.watchvault.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Plain unit test: no Spring, no database. */
class LoginRateLimiterTest {

	private static final int MAX_FAILED_LOGINS = 3;
	private static final String CLIENT = "203.0.113.7";

	private LoginRateLimiter limiter;

	@BeforeEach
	void setUp() {
		AuthProperties properties = new AuthProperties("unused", Duration.ofHours(1), "unused", true,
				MAX_FAILED_LOGINS, Duration.ofMinutes(15));
		limiter = new LoginRateLimiter(properties);
	}

	@Test
	void notBlockedBelowTheLimit() {
		failTimes(MAX_FAILED_LOGINS - 1);

		assertThat(limiter.isBlocked(CLIENT)).isFalse();
	}

	@Test
	void blockedOnceTheLimitIsReached() {
		failTimes(MAX_FAILED_LOGINS);

		assertThat(limiter.isBlocked(CLIENT)).isTrue();
	}

	@Test
	void successfulLoginClearsTheCount() {
		failTimes(MAX_FAILED_LOGINS - 1);
		limiter.recordSuccess(CLIENT);
		failTimes(MAX_FAILED_LOGINS - 1);

		assertThat(limiter.isBlocked(CLIENT)).isFalse();
	}

	@Test
	void clientsAreCountedSeparately() {
		failTimes(MAX_FAILED_LOGINS);

		assertThat(limiter.isBlocked("198.51.100.1")).isFalse();
	}

	private void failTimes(int times) {
		for (int i = 0; i < times; i++) {
			limiter.recordFailure(CLIENT);
		}
	}
}

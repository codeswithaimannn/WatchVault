package com.watchvault.auth;

import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Component;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * Slows down password guessing: after too many failed logins from one IP address,
 * further attempts are refused until the window ends.
 *
 * Counted per IP, not per email, so nobody can lock a victim out just by typing their email
 * wrong on purpose. In memory, so counts reset on restart; fine for one server.
 */
@Component
public class LoginRateLimiter {

	/** Upper bound on remembered addresses, so a flood of IPs cannot exhaust memory. */
	private static final int MAX_TRACKED_CLIENTS = 10_000;

	private final Cache<String, AtomicInteger> failuresByClient;
	private final int maxFailedLogins;

	public LoginRateLimiter(AuthProperties properties) {
		this.maxFailedLogins = properties.maxFailedLogins();
		// The window starts at the first failure: the entry expires that long after it was created.
		this.failuresByClient = Caffeine.newBuilder()
				.expireAfterWrite(properties.failedLoginWindow())
				.maximumSize(MAX_TRACKED_CLIENTS)
				.build();
	}

	public boolean isBlocked(String clientAddress) {
		AtomicInteger failures = failuresByClient.getIfPresent(clientAddress);
		return failures != null && failures.get() >= maxFailedLogins;
	}

	public void recordFailure(String clientAddress) {
		failuresByClient.get(clientAddress, address -> new AtomicInteger()).incrementAndGet();
	}

	public void recordSuccess(String clientAddress) {
		failuresByClient.invalidate(clientAddress);
	}
}

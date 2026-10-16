package com.watchvault.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Runs against the real MySQL test database (watchvault_test), not an in-memory one.
 * Each test runs in a transaction that is rolled back, so the database stays empty.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserRepositoryTest {

	private static final String HASH = "not-a-real-hash";

	private final UserRepository users;

	@Autowired
	UserRepositoryTest(UserRepository users) {
		this.users = users;
	}

	@Test
	void savedUserCanBeFoundByEmail() {
		users.saveAndFlush(new User("alice@example.com", "Test User", HASH));

		User found = users.findByEmail("alice@example.com").orElseThrow();

		assertThat(found.getId()).isNotNull();
		assertThat(found.getRole()).isEqualTo(Role.USER);
		assertThat(found.getCreatedAt()).isNotNull();
		assertThat(found.getUpdatedAt()).isNotNull();
	}

	@Test
	void emailIsTrimmedAndLowercased() {
		User saved = users.saveAndFlush(new User("  Bob@Example.COM ", "Test User", HASH));

		assertThat(saved.getEmail()).isEqualTo("bob@example.com");
	}

	@Test
	void duplicateEmailIsRejectedByTheDatabase() {
		users.saveAndFlush(new User("carol@example.com", "Test User", HASH));

		assertThatThrownBy(() -> users.saveAndFlush(new User("carol@example.com", "Test User", HASH)))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void existsByEmailReportsWhetherAnAccountExists() {
		users.saveAndFlush(new User("dave@example.com", "Test User", HASH));

		assertThat(users.existsByEmail("dave@example.com")).isTrue();
		assertThat(users.existsByEmail("nobody@example.com")).isFalse();
	}

	@Test
	void unknownEmailReturnsEmpty() {
		assertThat(users.findByEmail("nobody@example.com")).isEmpty();
	}
}

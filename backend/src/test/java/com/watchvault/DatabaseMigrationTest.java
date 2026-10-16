package com.watchvault;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class DatabaseMigrationTest {

	private final JdbcTemplate jdbc;

	@Autowired
	DatabaseMigrationTest(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	// Safety net: if test config ever points at the real database, fail loudly.
	@Test
	void testsRunAgainstTheTestDatabase() {
		String database = jdbc.queryForObject("SELECT DATABASE()", String.class);
		assertThat(database).isEqualTo("watchvault_test");
	}

	@Test
	void migrationV1Succeeded() {
		Boolean success = jdbc.queryForObject(
				"SELECT success FROM flyway_schema_history WHERE version = '1'", Boolean.class);
		assertThat(success).isTrue();
	}

	@Test
	void migrationV2AddedDisplayName() {
		String nullable = jdbc.queryForObject("""
				SELECT is_nullable FROM information_schema.columns
				WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'display_name'""",
				String.class);
		assertThat(nullable).isEqualTo("NO");
	}

	@Test
	void userTablesExist() {
		List<String> tables = jdbc.queryForList("""
				SELECT table_name FROM information_schema.tables
				WHERE table_schema = DATABASE()""", String.class);
		assertThat(tables).contains("users", "user_preferences",
				"user_preferred_languages", "user_preferred_genres");
	}

}

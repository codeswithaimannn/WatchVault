package com.watchvault.user;

import java.time.Instant;
import java.util.Locale;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A registered account. The schema is owned by Flyway (V1); this class only maps it.
 * Never returned from the API directly; controllers use DTOs.
 */
@Entity
@Table(name = "users")
public class User {

	/** Matches users.display_name VARCHAR(50) in the V2 migration. */
	public static final int MAX_DISPLAY_NAME_LENGTH = 50;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 254)
	private String email;

	/** Shown in the app ("Welcome, Sam"). User-written text: React escapes it when displayed. */
	@Column(name = "display_name", nullable = false, length = MAX_DISPLAY_NAME_LENGTH)
	private String displayName;

	@Column(name = "password_hash", nullable = false)
	private String passwordHash;

	// Stored as text ("USER"), matching the VARCHAR column. Without @JdbcTypeCode,
	// Hibernate expects a native MySQL ENUM column and schema validation fails.
	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(nullable = false, length = 20)
	private Role role;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	/** Required by JPA. Not for application code. */
	protected User() {
	}

	public User(String email, String displayName, String passwordHash) {
		this.email = normalizeEmail(email);
		this.displayName = displayName.trim();
		this.passwordHash = passwordHash;
		this.role = Role.USER;
	}

	/** Trims and lowercases so "Bob@Example.com " and "bob@example.com" are the same account. */
	public static String normalizeEmail(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}

	public Long getId() {
		return id;
	}

	public String getEmail() {
		return email;
	}

	public String getDisplayName() {
		return displayName;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public Role getRole() {
		return role;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}
}

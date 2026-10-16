package com.watchvault.user;

/** Account roles. Must match the CHECK constraint on users.role (V1 migration). */
public enum Role {
	USER,
	ADMIN
}

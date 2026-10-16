package com.watchvault.user;

import org.springframework.data.jpa.repository.JpaRepository;

/** Keyed by user id (shared primary key), so findById(userId) loads a user's preferences. */
public interface UserPreferencesRepository extends JpaRepository<UserPreferences, Long> {
}

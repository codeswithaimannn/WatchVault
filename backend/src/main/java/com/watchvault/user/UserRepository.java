package com.watchvault.user;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data writes the SQL for these from the method names. Pass emails already normalised. */
public interface UserRepository extends JpaRepository<User, Long> {

	Optional<User> findByEmail(String email);

	boolean existsByEmail(String email);
}

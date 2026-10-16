package com.watchvault.user;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

	private final UserRepository users;

	public UserService(UserRepository users) {
		this.users = users;
	}

	@Transactional(readOnly = true)
	public User getById(Long userId) {
		return users.findById(userId).orElseThrow(AccountNotFoundException::new);
	}
}

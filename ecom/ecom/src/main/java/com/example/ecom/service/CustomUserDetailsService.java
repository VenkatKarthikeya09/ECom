package com.example.ecom.service;

import com.example.ecom.model.User;
import com.example.ecom.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

@Service
public class CustomUserDetailsService implements UserDetailsService {

	private final UserRepository userRepository;

	@Autowired
	public CustomUserDetailsService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Override
	public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
		Optional<User> userOptional;
		if (usernameOrEmail.contains("@")) {
			userOptional = userRepository.findByEmail(usernameOrEmail);
		} else {
			userOptional = userRepository.findByUsername(usernameOrEmail);
		}
		User user = userOptional.orElseThrow(() -> new UsernameNotFoundException("User not found with username or email: " + usernameOrEmail));
		return org.springframework.security.core.userdetails.User.builder()
			.username(user.getUsername())
			.password(user.getPassword())
			.authorities(getAuthorities(user))
			.build();
	}

	private Collection<? extends GrantedAuthority> getAuthorities(User user) {
		return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
	}
} 
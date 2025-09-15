package com.example.ecom.service;

import com.example.ecom.model.User;
import com.example.ecom.model.Role;
import com.example.ecom.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Registers a new user with the default CUSTOMER role.
     * @param user The user object to be registered.
     * @return The saved user object, or an empty Optional if a user with the same
     * username or email already exists.
     */
    public Optional<User> registerNewUser(User user) {
        // Check if a user with the same username already exists
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            return Optional.empty();
        }

        // Check if a user with the same email already exists
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            return Optional.empty();
        }

        // Securely hash the password before saving
        String encodedPassword = passwordEncoder.encode(user.getPassword());
        user.setPassword(encodedPassword);

        // Set the default role for a new user to CUSTOMER
        user.setRole(Role.CUSTOMER);

        // Save the new user to the database
        User savedUser = userRepository.save(user);
        return Optional.of(savedUser);
    }

    /**
     * Finds a user by their username for authentication purposes.
     * @param username The username to search for.
     * @return An Optional containing the User if found.
     */
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }
}

package com.example.ecom.config;

import com.example.ecom.model.Role;
import com.example.ecom.model.User;
import com.example.ecom.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class AdminBootstrap implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        final String adminUsername = "admin";
        final String adminEmail = "admin@example.com";
        final String defaultPassword = "Admin@123";

        Optional<User> existingOpt = userRepository.findByUsername(adminUsername);
        if (existingOpt.isEmpty()) {
            User admin = new User();
            admin.setUsername(adminUsername);
            admin.setEmail(adminEmail);
            admin.setFirstName("Site");
            admin.setLastName("Admin");
            admin.setRole(Role.ADMIN);
            admin.setPassword(passwordEncoder.encode(defaultPassword));
            userRepository.save(admin);
            System.out.println("[Bootstrap] Admin user created: admin / Admin@123");
        } else {
            User admin = existingOpt.get();
            admin.setRole(Role.ADMIN);
            if (admin.getEmail() == null || admin.getEmail().isBlank()) {
                admin.setEmail(adminEmail);
            }
            admin.setPassword(passwordEncoder.encode(defaultPassword));
            userRepository.save(admin);
            System.out.println("[Bootstrap] Admin user reset: admin / Admin@123");
        }
    }
} 
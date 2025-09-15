package com.example.ecom.controller;

import com.example.ecom.model.User;
import com.example.ecom.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Controller
public class UserController {

	private final UserService userService;
	private final PasswordEncoder passwordEncoder;

	@Autowired
	public UserController(UserService userService, PasswordEncoder passwordEncoder) {
		this.userService = userService;
		this.passwordEncoder = passwordEncoder;
	}

	@GetMapping("/register")
	public String showRegistrationForm(Model model) {
		model.addAttribute("user", new User());
		return "signup";
	}

	@PostMapping("/register")
	public ResponseEntity<?> registerUser(@RequestBody User user) {
		Optional<User> registeredUser = userService.registerNewUser(user);
		if (registeredUser.isPresent()) {
			return ResponseEntity.ok("User registered successfully");
		} else {
			return ResponseEntity.status(HttpStatus.CONFLICT).body("Username or email already exists. Please choose a different one.");
		}
	}

	@GetMapping("/login")
	public String showLoginPage() {
		return "login";
	}

	@GetMapping("/dashboard")
	public String adminDashboard() {
		return "dashboard";
	}

	@GetMapping("/api/auth/status")
	@ResponseBody
	public Map<String, Object> authStatus(Authentication authentication) {
		Map<String, Object> resp = new HashMap<>();
		boolean authenticated = authentication != null && authentication.isAuthenticated();
		resp.put("authenticated", authenticated);
		return resp;
	}
} 
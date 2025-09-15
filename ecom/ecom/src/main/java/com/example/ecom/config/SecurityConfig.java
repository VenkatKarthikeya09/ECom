package com.example.ecom.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.beans.factory.annotation.Autowired;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Autowired
	private UserDetailsService userDetailsService;

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
		return config.getAuthenticationManager();
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
			.authenticationProvider(daoAuthenticationProvider())
			.authorizeHttpRequests(authorize -> authorize
				.requestMatchers("/register", "/login", "/css/**", "/js/**", "/images/**", "/img-proxy", "/", "/error", "/cart", "/update-quantity/**", "/remove-from-cart/**", "/api/products/**", "/api/auth/status", "/search", "/product/**", "/category/**").permitAll()
				.requestMatchers("/admin_home", "/admin/**", "/dashboard").hasAuthority("ROLE_ADMIN")
				.anyRequest().authenticated()
			)
			.formLogin(form -> form
				.loginPage("/login")
				.loginProcessingUrl("/login")
				.usernameParameter("username")
				.passwordParameter("password")
				.successHandler(authenticationSuccessHandler())
				.failureHandler(authenticationFailureHandler())
				.permitAll()
			)
			.logout(logout -> logout
				.logoutUrl("/logout")
				.logoutSuccessHandler(logoutSuccessHandler())
				.invalidateHttpSession(true)
				.deleteCookies("JSESSIONID")
				.permitAll()
			)
			.sessionManagement(session -> session
				.maximumSessions(1)
				.maxSessionsPreventsLogin(false)
			)
			.csrf(csrf -> csrf.disable());

		return http.build();
	}

	@Bean
	public org.springframework.security.authentication.dao.DaoAuthenticationProvider daoAuthenticationProvider() {
		org.springframework.security.authentication.dao.DaoAuthenticationProvider provider = new org.springframework.security.authentication.dao.DaoAuthenticationProvider();
		provider.setUserDetailsService(userDetailsService);
		provider.setPasswordEncoder(passwordEncoder());
		return provider;
	}

	@Bean
	public AuthenticationSuccessHandler authenticationSuccessHandler() {
		return (request, response, authentication) -> {
			boolean isAdmin = authentication.getAuthorities().stream()
				.anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
			if (isAdmin) {
				response.sendRedirect("/admin_home");
			} else {
				response.sendRedirect("/");
			}
		};
	}

	@Bean
	public AuthenticationFailureHandler authenticationFailureHandler() {
		return (request, response, exception) -> {
			request.getSession().setAttribute("error", "Invalid username or password.");
			response.sendRedirect("/login?error=true");
		};
	}

	@Bean
	public LogoutSuccessHandler logoutSuccessHandler() {
		return (request, response, authentication) -> {
			request.getSession().setAttribute("message", "You have been successfully logged out.");
			response.sendRedirect("/");
		};
	}
} 
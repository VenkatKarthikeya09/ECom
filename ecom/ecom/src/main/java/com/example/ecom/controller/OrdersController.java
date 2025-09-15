package com.example.ecom.controller;

import com.example.ecom.model.Orders;
import com.example.ecom.model.User;
import com.example.ecom.repository.OrdersRepository;
import com.example.ecom.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/orders")
public class OrdersController {

	@Autowired private OrdersRepository ordersRepository;
	@Autowired private UserRepository userRepository;

	@GetMapping
	public String list(Model model, Authentication auth) {
		if (auth == null || !auth.isAuthenticated()) {
			return "redirect:/login";
		}
		List<Orders> orders = new ArrayList<>();
		Optional<User> user = userRepository.findByUsername(auth.getName());
		if (user.isPresent()) {
			orders = ordersRepository.findByUser(user.get());
		}
		// Fallback: find by email on addresses if user linkage was missing
		if (orders.isEmpty() && user.isPresent()) {
			orders.addAll(ordersRepository.findByAddressEmail(user.get().getEmail()));
		}
		orders.sort(Comparator.comparing(Orders::getCreatedAt).reversed());
		model.addAttribute("orders", orders);
		return "orders/list";
	}

	@GetMapping("/{id}")
	public String detail(@PathVariable Integer id, Model model, Authentication auth) {
		if (auth == null || !auth.isAuthenticated()) {
			return "redirect:/login";
		}
		Optional<Orders> opt = ordersRepository.findById(id);
		if (opt.isEmpty()) return "redirect:/orders";
		Orders order = opt.get();
		model.addAttribute("order", order);
		model.addAttribute("items", order.getItems());
		return "orders/detail";
	}
} 